package com.chuhezhe.ragknowledgeservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chuhezhe.common.constants.ErrorConstants;
import com.chuhezhe.common.entity.Result;
import com.chuhezhe.common.util.FileUtil;
import com.chuhezhe.ragcommonservice.dto.QueryDocDTO;
import com.chuhezhe.ragcommonservice.dto.UploadDocDTO;
import com.chuhezhe.ragcommonservice.vo.DocumentVO;
import com.chuhezhe.ragcommonservice.vo.UploadDocResult;
import com.chuhezhe.ragcommonservice.vo.KnowledgeBaseVO;
import com.chuhezhe.ragcommonservice.vo.UserVO;
import com.chuhezhe.ragknowledgeservice.dto.KnowledgeBaseDTO;
import com.chuhezhe.ragknowledgeservice.entity.KnowledgeBase;
import com.chuhezhe.ragknowledgeservice.feign.DocumentServiceClient;
import com.chuhezhe.ragknowledgeservice.mapper.KnowledgeBaseMapper;
import com.chuhezhe.ragknowledgeservice.vo.UploadKBDocVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> {

    // 文档服务feign客户端
    private final DocumentServiceClient  documentServiceClient;

    /**
     * 获取用户的知识库
     */
     public Result<List<KnowledgeBaseVO>> getKnowledgeBases(UserVO user, Integer kbId) {
        // 从数据库查询用户的知识库
        List<KnowledgeBase> knowledgeBases = baseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getUserId, user.getId())
                        .eq(kbId != null, KnowledgeBase::getId, kbId)
        );

        // 转换为VO
        List<KnowledgeBaseVO> knowledgeBaseVOS = knowledgeBases.stream()
                .map(k -> new KnowledgeBaseVO(k.getName(), k.getDescription()))
                .collect(Collectors.toList());

        return Result.ok(knowledgeBaseVOS);
    }

    /**
     * 创建知识库
     */
     public Result<KnowledgeBaseVO> createKnowledgeBase(UserVO user, KnowledgeBaseDTO knowledgeBaseDTO) {
        // 转换为实体
        KnowledgeBase knowledgeBase = KnowledgeBase.fromDTO(knowledgeBaseDTO);
        knowledgeBase.setUserId(user.getId());

        // 保存到数据库
        baseMapper.insert(knowledgeBase);

        // 返回VO
        return Result.ok(new KnowledgeBaseVO(knowledgeBase.getName(), knowledgeBase.getDescription()));
    }

    /**
     * 删除知识库
     */
     public Result<Void> deleteKnowledgeBase(UserVO user, Long kbId) {
         // 检查知识库是否存在
         KnowledgeBase knowledgeBase = new LambdaQueryChainWrapper<KnowledgeBase>(baseMapper)
                 .eq(KnowledgeBase::getId, kbId)
                 .eq(KnowledgeBase::getUserId, user.getId())
                 .one();

         if (knowledgeBase == null) {
             return Result.error(ErrorConstants.KNOWLEDGE_BASE_NOT_EXIST);
         }

         // 删除知识库
         baseMapper.deleteById(kbId);

         // TODO 删除向量库和minio中对应的记录

         return Result.ok();
     }

     /**
      * 更新知识库
      */
     public Result<Void> updateKnowledgeBase(UserVO user, Integer kbId, KnowledgeBaseDTO knowledgeBaseDTO) {
         // 检查知识库是否存在
         KnowledgeBase knowledgeBase = new LambdaQueryChainWrapper<>(baseMapper)
                 .eq(KnowledgeBase::getId, kbId)
                 .eq(KnowledgeBase::getUserId, user.getId())
                 .one();

         if (knowledgeBase == null) {
             return Result.error(ErrorConstants.KNOWLEDGE_BASE_NOT_EXIST);
         }

         // 更新知识库
         knowledgeBase.setName(knowledgeBaseDTO.getName());
         knowledgeBase.setDescription(knowledgeBaseDTO.getDescription());
         baseMapper.updateById(knowledgeBase);

         return Result.ok();
     }

    /**
     * 上传知识库文档
     */
     public Result<List<UploadKBDocVO>> uploadKnowledgeBaseDocuments(UserVO user, Integer kbId, List<MultipartFile> files) {
         // 检查知识库是否存在
         KnowledgeBase knowledgeBase = new LambdaQueryChainWrapper<>(baseMapper)
                 .eq(KnowledgeBase::getId, kbId)
                 .eq(KnowledgeBase::getUserId, user.getId())
                 .one();

         if (knowledgeBase == null) {
             return Result.error(ErrorConstants.KNOWLEDGE_BASE_NOT_EXIST);
         }

         List<UploadKBDocVO> results = new ArrayList<>();

         // 上传文档到minio
         for (MultipartFile multipartFile : files) {
             File file = new File("temp/" + multipartFile.getOriginalFilename());
             String fileHash;

             // 1. 计算文件hash
             try {
                 fileHash = FileUtil.calculateFileSHA256(file);
             }
             catch (IOException | NoSuchAlgorithmException e) {
                 log.error(e.getMessage());
                 return Result.error(ErrorConstants.INTERNAL_ERROR, e.getMessage());
             }

             // 2. 检查是否存在完全相同的文件
             Result<List<DocumentVO>> docs = documentServiceClient.queryDocument(new QueryDocDTO(kbId, fileHash, multipartFile.getOriginalFilename()));
             if (docs.isSuccess() && !docs.getData().isEmpty()){
                 DocumentVO documentVO = docs.getData().getFirst();

                 results.add(
                         UploadKBDocVO.builder()
                                 .documentId(documentVO.getId())
                                 .fileName(documentVO.getFileName())
                                 .status(UploadKBDocVO.Status.EXISTS)
                                 .message("文件已存在且处理完成")
                                 .skipProcessing(true)
                                 .build()
                 );

                 continue;
             }

             // 3. 上传文档到 minio
             // rag-document-service 的 /api/ai/documents/upload 在落 MinIO 的同时已经会创建 document_uploads 记录，
             // 并在 UploadDocResult.uploadDocRecordId 里把主键返回过来，不需要再发一次 /upload/record
             String tempPath = String.format("kb_%d/temp/%s", kbId, multipartFile.getOriginalFilename());
             Result<UploadDocResult> uploadRes = documentServiceClient.uploadDocument(
                     new UploadDocDTO(kbId, null, tempPath, multipartFile)
             );

             Integer uploadId = uploadRes != null && uploadRes.getData() != null
                     ? uploadRes.getData().getUploadDocRecordId()
                     : null;

             results.add(
                 UploadKBDocVO.builder()
                        .uploadId(uploadId)
                        .fileName(file.getName())
                        .tempPath(tempPath)
                        .status(UploadKBDocVO.Status.PENDING)
                        .skipProcessing(false)
                        .build()
            );
         }

         return Result.ok(results);
     }
}
