from typing import TypeVar, Generic, Optional

from pydantic import BaseModel

T = TypeVar("T")

class ApiResponse(BaseModel, Generic[T]):
    code: int = 200
    codeDesc: str = None
    message: str = "success"
    data: Optional[T] = None

    class Config:
        from_attributes = True  # 支持从对象属性解析（比如SQLAlchemy模型）

