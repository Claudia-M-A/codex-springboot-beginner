# Codex Spring Boot Beginner

Java 21 Spring Boot application. Maven builds an executable JAR for the AWS Elastic Beanstalk Java SE platform. The `/hello` endpoint returns a JSON greeting.

## Elastic Beanstalk deployment

The existing Elastic Beanstalk environment must use a compatible Java SE platform (Corretto 21). On a push to `main`, GitHub Actions runs tests, packages and validates one executable Spring Boot JAR, then sends that JAR to the existing environment. Elastic Beanstalk runs a single JAR with `java -jar`; the workflow uploads the JAR itself, not a WAR or a ZIP containing the project. AWS recommends a `Procfile` for explicit process configuration, but it is optional for a single JAR. [Java SE platform documentation](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/java-se-platform.html).

Pull requests targeting `main` run tests only. They do not package an artifact for deployment, request AWS credentials, or deploy. The deploy job receives `id-token: write` permission only on pushes to `main`.

### Workflow steps

The workflow is `.github/workflows/deploy-to-elastic-beanstalk.yml`:

1. **Trigger:** runs on pushes to `main` and pull requests targeting `main`.
2. **Build and test job:** checks out the repository, configures Temurin Java 21 with Maven dependency caching, makes the Maven wrapper executable, and runs `./mvnw clean test`.
3. **Package on main push:** runs `./mvnw package -DskipTests` after tests have passed.
4. **Verify JAR:** requires exactly one JAR, checks the Spring Boot launcher manifest and `BOOT-INF` application/dependency content, and verifies the embedded Tomcat runtime.
5. **Upload artifact:** stores the verified JAR as a GitHub Actions artifact for the deploy job.
6. **Deploy job:** runs only on a push to `main` after the build job succeeds. It downloads the JAR, obtains short-lived credentials by assuming the IAM role using GitHub OIDC, and runs `aws-actions/aws-elasticbeanstalk-deploy` against the existing application and environment.

The deployment action is configured not to create an Elastic Beanstalk application, environment, or S3 bucket. It does upload the application bundle to the existing bucket, create an Elastic Beanstalk application version, and update the existing environment to that version. It does not provision or modify the environment's underlying infrastructure.

### GitHub configuration

Set these repository values:

| Name | GitHub setting | Value |
| --- | --- | --- |
| `AWS_ROLE_TO_ASSUME` | Actions secret | ARN of the OIDC deployment role |
| `EB_ARTIFACT_BUCKET` | Actions variable | Existing S3 bucket for deployment bundles in `ap-southeast-2` |

The workflow is configured for AWS region `ap-southeast-2`, Elastic Beanstalk application `codex-springboot-beginner`, and environment `codex-springboot-beginner-env`.

The IAM role trust policy should accept tokens from GitHub's OIDC provider `token.actions.githubusercontent.com`, require audience `sts.amazonaws.com`, and restrict the subject to `repo:Claudia-M-A/codex-springboot-beginner:ref:refs/heads/main`. That prevents pull-request runs from assuming the deployment role. No permanent AWS access keys are used.

### IAM permissions for the GitHub deployment role

Scope permissions to the existing application, environment, and artifact bucket/object prefix wherever AWS supports resource-level restrictions:

| Permission | Resource / reason |
| --- | --- |
| `elasticbeanstalk:CreateApplicationVersion` | Existing application ARN and application-version ARN for this application's versions. Registers each uploaded JAR as an application version. |
| `elasticbeanstalk:UpdateEnvironment` | Existing environment ARN; constrain with `elasticbeanstalk:InApplication` to this application. |
| `elasticbeanstalk:DescribeEnvironments` | Existing environment ARN; the action checks environment status. |
| `elasticbeanstalk:DescribeApplicationVersions` | Application-version ARNs for this application; the action checks whether the commit version already exists. |
| `s3:ListBucket` | Existing artifact bucket ARN; needed by the action's bucket ownership/existence check (`HeadBucket`). |
| `s3:PutObject` | Only the artifact key prefix used by this application; uploads the JAR. |
| `s3:GetObject` | Same source object prefix; required for Elastic Beanstalk to read a source bundle in a custom S3 bucket. |

`sts:GetCallerIdentity` is called by the deployment action but AWS does not require an identity policy grant for that operation. Do not grant the GitHub role permissions to create or terminate environments, create applications or buckets, or pass IAM roles. The Elastic Beanstalk environment's service role and EC2 instance profile are separate roles and are not assumed by this workflow. See AWS's [Elastic Beanstalk service authorization reference](https://docs.aws.amazon.com/service-authorization/latest/reference/list_elasticbeanstalk.html) and [`CreateApplicationVersion` source-bundle permissions](https://docs.aws.amazon.com/elasticbeanstalk/latest/api/API_CreateApplicationVersion.html).

### Deployment and troubleshooting

After the existing AWS application, environment, S3 bucket, OIDC provider, and least-privilege role are configured, add the GitHub secret and variable above. Merge or push a change to `main` to deploy. A pull request only runs tests.

- **OIDC assume-role failure:** verify the OIDC provider, `sts.amazonaws.com` audience, exact branch subject, and `AWS_ROLE_TO_ASSUME` secret.
- **S3 or Elastic Beanstalk `AccessDenied`:** check the required action and resource ARN in the job log, especially bucket `s3:ListBucket`, artifact `s3:PutObject`/`s3:GetObject`, and the named environment/application permissions.
- **Artifact missing or invalid:** inspect the package and JAR verification steps. The workflow expects one Spring Boot executable JAR with embedded Tomcat.
- **Environment fails health checks:** inspect Elastic Beanstalk Events and instance logs in CloudWatch. Confirm the existing environment is Java SE Corretto 21 and the JAR starts successfully.

Run tests locally with `./mvnw clean test` (Windows: `mvnw.cmd clean test`).
