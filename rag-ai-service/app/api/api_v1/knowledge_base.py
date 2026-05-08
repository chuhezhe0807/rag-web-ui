"""
KB 域的 CRUD / upload / preview / process / cleanup / 文档列表等端点在 US-007
统一迁到 Java 侧（rag-knowledge-service 的 /knowledge-base/**、rag-document-service
的 /api/ai/documents/**），由网关独立路由；Python 侧这里只保留 chat 域需要的
retrieval 入口 /knowledge-base/test-retrieval。

如果未来 chat 模块需要新的"读检索/向量库"型接口，可以加在本文件；KB 写操作
不要再回到这里。
"""
from typing import Any

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.security import get_current_user
from app.db.session import get_db
from app.models.knowledge import KnowledgeBase
from app.models.user import User
from app.services.embedding.embedding_factory import EmbeddingsFactory
from app.services.vector_store import VectorStoreFactory

router = APIRouter()


class TestRetrievalRequest(BaseModel):
    query: str
    kb_id: int
    top_k: int


@router.post("/test-retrieval")
async def test_retrieval(
    request: TestRetrievalRequest,
    background_tasks: BackgroundTasks,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> Any:
    """Test retrieval quality for a given query against a knowledge base."""
    try:
        kb = (
            db.query(KnowledgeBase)
            .filter(
                KnowledgeBase.id == request.kb_id,
                KnowledgeBase.user_id == current_user.id,
            )
            .first()
        )

        if not kb:
            raise HTTPException(
                status_code=404,
                detail=f"Knowledge base {request.kb_id} not found",
            )

        embeddings = EmbeddingsFactory.create()

        vector_store = VectorStoreFactory.create(
            store_type=settings.VECTOR_STORE_TYPE,
            collection_name=f"kb_{request.kb_id}",
            embedding_function=embeddings,
        )

        results = vector_store.similarity_search_with_score(request.query, k=request.top_k)

        response = []
        for doc, score in results:
            response.append(
                {
                    "content": doc.page_content,
                    "metadata": doc.metadata,
                    "score": float(score),
                }
            )

        return {"results": response}

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
