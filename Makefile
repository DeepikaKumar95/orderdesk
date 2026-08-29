.PHONY: up down logs build test e2e
up:        ## start SQL Server, Redpanda, Jaeger and both services
	docker compose -f deploy/docker-compose.yml up --build -d
down:
	docker compose -f deploy/docker-compose.yml down -v
logs:
	docker compose -f deploy/docker-compose.yml logs -f order-service inventory-service
build:
	cd services/order-service && mvn -q -DskipTests package
	cd services/inventory-service && mvn -q -DskipTests package
test:
	cd services/order-service && mvn test
	cd services/inventory-service && mvn test
e2e:
	cd e2e && npm ci && npx playwright test
