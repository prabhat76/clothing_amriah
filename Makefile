.PHONY: help build run test clean docker-up docker-down

help:
	@echo "Targets: build, run, test, clean, docker-up, docker-down"

build:
	mvn clean package -DskipTests

run:
	mvn spring-boot:run

test:
	mvn test

clean:
	mvn clean

docker-up:
	docker compose up -d

docker-down:
	docker compose down -v
