# dat-was-sys Mock Tool Design

## Scope

Add two read-only system mock tools to `dat-was-sys`, following the metadata shape of `smp_team_list`.

## Tools

- `sys_system_status`: optional `environment` input; returns a mock system name, environment, status, and checked timestamp.
- `sys_notice_list`: optional `category` input; returns a mock list of system notices with title, priority, and published date.

## Resources

For each tool, add:

- A YAML definition under `src/main/resources/tool-definitions/sys/`.
- A corresponding successful JSON response under `src/main/resources/mock-responses/`.

Both tools are read-only, non-destructive, and require no environment keys. Responses contain only synthetic operational data and no personal or secret information.

## Validation

Add a resource-focused test that loads the two YAML definitions and verifies their associated mock response files are available and valid JSON.
