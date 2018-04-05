all: docker-build-web

.PHONY: uberjar-build-web
uberjar-build-web:
	lein with-profile web ring uberjar

.PHONY: docker-build-web
docker-build-web: uberjar-build-web
	docker build -t wormbase/search-web-api -f ./docker/Dockerfile.web .

.PHONY: docker-run-web
docker-run-web:
	docker run -p 3000:3000 -e WB_DB_URI=${WB_DB_URI} wormbase/search-web-api

.PHONY: docker-build-aws-es
docker-build-aws-es:
	docker build -t wormbase/aws-elasticsearch -f ./docker/Dockerfile.aws-elasticsearch .

.PHONY: docker-run-aws-es
docker-run-aws-es:
	@docker run -p 9200:9200 wormbase/aws-elasticsearch \
		-Des.cloud.aws.access_key=${AWS_ACCESS_KEY_ID} \
		-Des.cloud.aws.secret_key=${AWS_SECRET_ACCESS_KEY}

.PHONY: eb-local-run
eb-local-run:
	@(cd eb/default && eb local run --envvars AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID},AWS_SECRET_KEY=${AWS_SECRET_ACCESS_KEY},WB_DB_URI=${WB_DB_URI})

.PHONY: eb-setenv
eb-setenv:
	@eb setenv AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} AWS_SECRET_KEY=${AWS_SECRET_ACCESS_KEY} WB_DB_URI=${WB_DB_URI}


.PHONY: aws-ecr-login
aws-ecr-login:
	aws ecr get-login --no-include-email --region us-east-1 | sh
