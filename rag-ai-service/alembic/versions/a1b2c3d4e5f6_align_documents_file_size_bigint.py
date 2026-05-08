"""align documents.file_size to BIGINT NOT NULL (US-008)

Revision ID: a1b2c3d4e5f6
Revises: 3580c0dcd005
Create Date: 2026-05-08 10:00:00.000000

initial_schema 把 documents.file_size 建成了 sa.Integer() 且 nullable，
但 Python ORM (app/models/knowledge.py) 和 Java 侧 Document.fileSize
(java.math.BigInteger) 都按 BIGINT NOT NULL 使用；超过 2^31 字节 (~2GiB)
的文件会在插入时溢出，或在 Java 反序列化时被截断。本迁移把 MySQL 列对齐
到 BIGINT NOT NULL。
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import mysql


revision: str = 'a1b2c3d4e5f6'
down_revision: Union[str, None] = '3580c0dcd005'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.alter_column(
        'documents',
        'file_size',
        existing_type=mysql.INTEGER(),
        type_=sa.BigInteger(),
        nullable=False,
    )


def downgrade() -> None:
    op.alter_column(
        'documents',
        'file_size',
        existing_type=sa.BigInteger(),
        type_=mysql.INTEGER(),
        nullable=True,
    )
