# dat-was-sys Executable Mock Tools Design

## Scope

Implement `sys_system_status` and `sys_notice_list` as executable, read-only MCP tools and REST endpoints in `dat-was-sys`.

## Architecture

Each tool has a request DTO, response DTO, `@McpTool` UseCase interface, and Spring `@Service` implementation. The implementation returns deterministic synthetic data without external calls. A REST controller delegates to the same UseCases, so MCP and HTTP return the same DTO contracts.

## Contracts

- `sys_system_status`: optional `environment`; returns `systemName`, `environment`, `status`, and `checkedAt`.
- `sys_notice_list`: optional `category`; returns a list of notices with `title`, `priority`, and `publishedDate`.
- REST endpoints: `GET /api/sys/status` and `GET /api/sys/notices` with their matching optional query parameters.

## Safety

Both tools remain read-only, non-destructive, and idempotent. All data is synthetic; no credentials, personal data, or external integrations are used.

## Validation

Unit tests will verify each UseCase response. MockMvc tests will verify endpoint success, optional parameter handling, and the exposed response fields. Existing YAML definitions and JSON mock responses remain aligned with the DTO contracts.
