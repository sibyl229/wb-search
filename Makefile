all: docker-build-web

.PHONY: uberjar-build-web
uberjar-build-web:
	lein with-profile web ring uberjar

.PHONY: docker-build-web
docker-build-web: uberjar-build-web
	docker build -t wb-es-web -f ./docker/Dockerfile.web .

.PHONY: docker-run-web
docker-run-web:
	docker run -p 3000:3000 wb-es-web

.PHONY: docker-build-aws-es
docker-build-aws-es:
	docker build -t wb-es-aws-elasticsearch -f ./docker/Dockerfile.aws-elasticsearch .

.PHONY: docker-run-aws-es
docker-run-aws-es:
	@docker run -p 9200:9200 wb-es-aws-elasticsearch \
		-Des.cloud.aws.access_key=${AWS_ACCESS_KEY_ID} \
		-Des.cloud.aws.secret_key=${AWS_SECRET_ACCESS_KEY}
