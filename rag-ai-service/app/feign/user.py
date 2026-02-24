from typing import Optional

from fastapi.params import Depends

from app.core.config import settings
from app.feign.feign_client import FeignClient
from app.models import User

user_service_feign: Optional[FeignClient] = None

def get_user_service_feign() -> FeignClient:
    global user_service_feign

    if user_service_feign is None:
        user_service_feign = FeignClient(
            service_name=settings.USER_SERVICE_NAME,
            default_headers={
                "Content-Type": "application/json"
            }
        )

    return user_service_feign

class UserServiceFeign:
    """
    用户服务feign调用

    """

    @staticmethod
    def get_current_user(token: str, feign_client: FeignClient = Depends(get_user_service_feign)) -> User:
        if token is None:
            raise ValueError("token is None")

        res = feign_client.call(
            path=f"{settings.USER_SERVICE_NAME}{settings.USER_SERVICE_API_VERSION}",
            headers={
                "Authorization": f"Bearer {token}"
            }
        )

        return User(**res)
