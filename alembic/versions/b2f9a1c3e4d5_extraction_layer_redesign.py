"""extraction layer redesign

Revision ID: b2f9a1c3e4d5
Revises: a510d6626fd1
Create Date: 2026-03-17

- proof_artifacts: add capability_tags, extraction_method; add 'pdf' to artifact_type_enum
- capability_declarations: replace fixed boolean fields with flexible capabilities JSON;
  add custom_description
"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "b2f9a1c3e4d5"
down_revision: Union[str, None] = "a510d6626fd1"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # ── proof_artifacts: new extraction columns ───────────────────────────────
    op.add_column(
        "proof_artifacts",
        sa.Column("capability_tags", sa.JSON(), nullable=False, server_default="[]"),
    )
    op.add_column(
        "proof_artifacts",
        sa.Column("extraction_method", sa.String(100), nullable=True),
    )

    # Add 'pdf' to the artifact_type_enum (PostgreSQL only supports ADD VALUE)
    op.execute("ALTER TYPE artifact_type_enum ADD VALUE IF NOT EXISTS 'pdf'")

    # ── capability_declarations: drop old fixed boolean columns ───────────────
    op.drop_column("capability_declarations", "rag_capability")
    op.drop_column("capability_declarations", "rag_detail")
    op.drop_column("capability_declarations", "agent_systems")
    op.drop_column("capability_declarations", "agent_systems_detail")
    op.drop_column("capability_declarations", "tool_orchestration")
    op.drop_column("capability_declarations", "tool_orchestration_detail")
    op.drop_column("capability_declarations", "memory_systems")
    op.drop_column("capability_declarations", "memory_systems_detail")

    # Add flexible capabilities JSON column
    op.add_column(
        "capability_declarations",
        sa.Column("capabilities", sa.JSON(), nullable=False, server_default="[]"),
    )
    op.add_column(
        "capability_declarations",
        sa.Column("custom_description", sa.Text(), nullable=True),
    )


def downgrade() -> None:
    # Restore old fixed columns
    op.add_column("capability_declarations", sa.Column("rag_capability", sa.Boolean(), nullable=False, server_default="false"))
    op.add_column("capability_declarations", sa.Column("rag_detail", sa.Text(), nullable=True))
    op.add_column("capability_declarations", sa.Column("agent_systems", sa.Boolean(), nullable=False, server_default="false"))
    op.add_column("capability_declarations", sa.Column("agent_systems_detail", sa.Text(), nullable=True))
    op.add_column("capability_declarations", sa.Column("tool_orchestration", sa.Boolean(), nullable=False, server_default="false"))
    op.add_column("capability_declarations", sa.Column("tool_orchestration_detail", sa.Text(), nullable=True))
    op.add_column("capability_declarations", sa.Column("memory_systems", sa.Boolean(), nullable=False, server_default="false"))
    op.add_column("capability_declarations", sa.Column("memory_systems_detail", sa.Text(), nullable=True))

    op.drop_column("capability_declarations", "capabilities")
    op.drop_column("capability_declarations", "custom_description")
    op.drop_column("proof_artifacts", "capability_tags")
    op.drop_column("proof_artifacts", "extraction_method")
