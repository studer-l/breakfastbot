# Container based development / deployment

Each sub folder describes a docker container. Service discovery for development
is done using a local docker network (`docker network create localnet`).

Note that deployment does not require docker, but it simplifies the process.

## `deploy/app`

The actual Breakfastbot application. Meant for deployment, relies on presence
of a `/breakfastbot/prod-config.edn`, you can either mount it or add in a
dockerfile. Available on
[dockerhub](https://cloud.docker.com/u/studerl/repository/docker/studerl/breakfastbot).

## `deploy/db`

A postgres container. Useful for both deployment and local development.

## `deploy/adminer`

Adminer container for debugging and development.
