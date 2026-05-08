package com.chuhezhe.ragknowledgeservice.feign;

import com.chuhezhe.common.entity.Result;
import com.chuhezhe.ragcommonservice.dto.QueryDocDTO;
import com.chuhezhe.ragcommonservice.dto.UploadDocDTO;
import com.chuhezhe.ragcommonservice.vo.DocumentVO;
import com.chuhezhe.ragcommonservice.vo.UploadDocResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("rag-document-service")
public interface DocumentServiceClient {

    /**
     * 查询文档
     */
    @GetMapping("/api/ai/documents/query")
     Result<List<DocumentVO>> queryDocument(@RequestBody QueryDocDTO queryDocDTO);

    /**
     * 上传文档到 MinIO 并创建上传记录，返回 UploadDocResult.uploadDocRecordId
     * （历史上这里还有一个 POST /api/ai/documents/upload/record，已按 US-006 删掉：
     * /upload 已经在同一事务里建记录并回传 id，多一次 RPC 只会产生重复记录）
     */
    @PostMapping("/api/ai/documents/upload")
    Result<UploadDocResult> uploadDocument(@RequestBody UploadDocDTO uploadDocDTO);
}
