# Nacos Config

Import each `*.yaml` file into Nacos with:

- Group: `DEFAULT_GROUP`
- Data ID: file name, for example `english-learning-common.yaml`
- Format: `YAML`

`english-learning-common.yaml` is imported first by every backend module.
Each service then imports its own Data ID, such as `user-service.yaml`.

Docker services use `NACOS_SERVER_ADDR=host.docker.internal:8848` so they can read a Nacos server running on the Windows host.
