import socket
from typing import Optional

from loguru import logger
from app.core.config import settings

from v2.nacos import NacosNamingService, ClientConfigBuilder, GRPCConfig, \
    RegisterInstanceParam, DeregisterInstanceParam

# 全局存储 Nacos 客户端实例（服务发现用）
global_nacos_naming_client: Optional[NacosNamingService] = None

def get_local_ip() -> str:
    """获取本地内网IP"""
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("8.8.8.8", 80))  # 不会真的发包
        return s.getsockname()[0]
    finally:
        s.close()

class BasicNacosRegistrar:

    def __init__(self):
        self._registered = False

    @staticmethod
    async def register(service_name: str, port: int, enabled: bool = False):
        global global_nacos_naming_client

        if not enabled:
            logger.info(f"Skip Nacos registration for service={service_name}, port={port}")
            return

        if global_nacos_naming_client:
            return global_nacos_naming_client

        ip_or_host = get_local_ip()

        client_config = (ClientConfigBuilder()
                         .server_address(settings.NACOS_SERVER_ADDR).namespace_id(settings.NACOS_NAMESPACE)
                         .username(settings.NACOS_USERNAME).password(settings.NACOS_PASSWORD)
                         .log_level('INFO')
                         .grpc_config(GRPCConfig(grpc_timeout=5000, max_keep_alive_ms=60000))
                         .build())

        global_nacos_naming_client = await NacosNamingService.create_naming_service(client_config)

        response = await global_nacos_naming_client.register_instance(
            request=RegisterInstanceParam(
                service_name=service_name,
                group_name=settings.NACOS_GROUP,
                ip=ip_or_host,
                port=port,
                ephemeral=True
            )
        )

        if response:
            logger.info(
                f"✅ Registered service={service_name}, host={ip_or_host}, port={port} to Nacos successfully"
            )
        else:
            logger.error(
                f"❌ Failed to register service={service_name}, host={ip_or_host}, port={port} to Nacos"
            )
        return global_nacos_naming_client

    @staticmethod
    async def deregister(nacos_naming_service: NacosNamingService,service_name: str, port: int):
        if nacos_naming_service:
            ip_or_host = get_local_ip()
            await nacos_naming_service.deregister_instance(
                request=DeregisterInstanceParam(service_name=service_name,
                                                group_name=settings.SERVICE_GROUP,
                                                ip=ip_or_host,
                                                port=port,
                                                ephemeral=True)
            )
