install:
	sbt compile

bootstrap: teardown
	docker-compose run --rm kafka-cli

integration-test: bootstrap start-app integration-test-no-bootstrap
	@# need to stop manually as `teardown` is included in bootstrap
	@# and Makefile does not execute a recipe twice
	docker-compose down --remove-orphans --volumes --timeout=5

integration-test-no-bootstrap:
	@echo "" && echo ""
	sbt cucumber
	@echo "" && echo ""

start-app:
	docker-compose build app-build-cache
	docker-compose up --build --detach app

	@echo "Waiting for the app"

	@until [ $${counter:-0} -gt 20 ]; \
	do \
		if docker-compose logs app | grep -q "Current state is: RUNNING" ; then \
			break; \
		fi; \
		counter=$$(($$counter+1)); \
	done;

	docker-compose logs app | grep -q "Current state is: RUNNING" || (docker-compose logs app; exit 1)
	@echo "started and ready"

unit-test:
	@echo "" && echo ""
	sbt test
	@echo "" && echo ""

tests: unit-test integration-test

teardown:
	docker-compose down --remove-orphans --volumes --timeout=5

