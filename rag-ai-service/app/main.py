import logging
from contextlib import asynccontextmanager

import uvicorn
from loguru import logger

from app.api.api_v1.api import api_router
from app.api.openapi.api import router as openapi_router
from app.core.config import settings
from app.core.minio import init_minio
from app.nacos.nacos_config import init_nacos_config, stop_nacos_config
from app.nacos.nacos_registry import BasicNacosRegistrar
from app.startup.migarate import DatabaseMigrator
from fastapi import FastAPI

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Initialize MinIO
    init_minio()
    # Run database migrations
    migrator = DatabaseMigrator(settings.get_database_url)
    migrator.run_migrations()

    # Nacos 配置监听 & 服务注册（同步）
    nacos_config_client = await init_nacos_config()
    naming_client = await BasicNacosRegistrar.register(service_name=settings.PROJECT_NAME, port=settings.APP_PORT, enabled=True)

    try:
        yield
    finally:
        try:
            stop_nacos_config(nacos_config_client)
        except Exception as e:
            logger.warning(f"Nacos remove watcher failed: {e}")
        try:
            await BasicNacosRegistrar.deregister(naming_client, service_name=settings.PROJECT_NAME, port=settings.APP_PORT)
        except Exception as e:
            logger.warning(f"Nacos deregister failed: {e}")

app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    openapi_url=f"{settings.API_V1_STR}/openapi.json",
    lifespan=lifespan,
)

# Include routers
app.include_router(api_router, prefix=settings.API_V1_STR)
app.include_router(openapi_router, prefix="/openapi")

@app.get("/")
def root():
    return {"message": "Welcome to RAG Web UI API"}


@app.get("/api/health")
async def health_check():
    return {
        "status": "healthy",
        "version": settings.VERSION,
    }

if __name__ == "__main__":
    # 编程式启动 uvicorn，传入端口
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=settings.APP_PORT,
        reload=True
    )