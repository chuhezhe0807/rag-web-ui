import random
from typing import Dict, List, Optional
import httpx
from fastapi import HTTPException
from v2.nacos import ListInstanceParam, Instance


class FeignClient:
    """改造后的 FeignClient，复用全局 Nacos Client"""

    def __init__(self, service_name: str, default_headers: Optional[Dict] = None):
        self.service_name = service_name
        self.instances: List[Instance] = []
        self.default_headers = default_headers or {
            "Content-Type": "application/json",
            "User-Agent": "FastAPI-Feign-Client"
        }

        # 不再新建 Nacos Client，而是引用全局实例
        from app.nacos.nacos_registry import global_nacos_naming_client
        self.nacos_client = global_nacos_naming_client
        if not self.nacos_client:
            raise RuntimeError("Nacos naming client not initialized!")

    async def _refresh_instances(self):
        """复用全局 Nacos Client 获取服务实例"""
        try:
            # 使用复用的 client 调用服务发现接口
            self.instances = await self.nacos_client.list_instances(
                ListInstanceParam(service_name=self.service_name, healthy_only=True)
            )
            if not self.instances:
                raise ValueError(f"未找到 {self.service_name} 的可用实例")
        except Exception as e:
            raise HTTPException(status_code=503, detail=f"服务发现失败: {str(e)}")

    def _get_instance(self) -> str:
        instance = random.choice(self.instances)
        return f"http://{instance.to_inet_addr()}"

    def _merge_headers(self, headers: Optional[Dict]) -> Dict:
        merged_headers = self.default_headers.copy()
        if headers:
            merged_headers.update(headers)
        return merged_headers

    async def async_call(self,
                         path: str,
                         method: str = "GET",
                         params: Dict = None,
                         json: Dict = None,
                         headers: Dict = None) -> Dict:
        await self._refresh_instances()
        base_url = self._get_instance()
        full_url = f"{base_url}{path}"
        final_headers = self._merge_headers(headers)

        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                response = await client.request(
                    method=method,
                    url=full_url,
                    params=params or {},
                    json=json or {},
                    headers=final_headers
                )
                response.raise_for_status()
                return response.json()
        except httpx.HTTPStatusError as e:
            raise HTTPException(status_code=e.response.status_code, detail=f"接口调用失败: {e.response.text}")
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"请求异常: {str(e)}")


    def call(self,
                         path: str,
                         method: str = "GET",
                         params: Dict = None,
                         json: Dict = None,
                         headers: Dict = None) -> Dict:
        base_url = self._get_instance()
        full_url = f"{base_url}{path}"
        final_headers = self._merge_headers(headers)

        try:
            with httpx.Client(timeout=10.0) as client:
                response = client.request(
                    method=method,
                    url=full_url,
                    params=params or {},
                    json=json or {},
                    headers=final_headers
                )
                response.raise_for_status()
                return response.json()
        except httpx.HTTPStatusError as e:
            raise HTTPException(status_code=e.response.status_code, detail=f"接口调用失败: {e.response.text}")
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"请求异常: {str(e)}")