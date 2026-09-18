# demo-api

A small Spring Boot REST API for a to-do style `Item` resource, backed by an
in-memory store (no database needed). Use it to practice hitting endpoints
with Postman or curl.

## Run

```bash
cd demo-api
mvn spring-boot:run
```

(or run `DemoApiApplication` from your IDE, same as the `DemoApplication`
project in your screenshot).

The app starts on `http://localhost:8080` and seeds one item with id `1`.

## Endpoints

| Method | URL                  | Body                                             | Description        |
|--------|----------------------|---------------------------------------------------|---------------------|
| GET    | `/api/items`         | -                                                 | List all items      |
| GET    | `/api/items/{id}`    | -                                                 | Get one item        |
| POST   | `/api/items`         | `{"name": "...", "description": "...", "done": false}` | Create an item |
| PUT    | `/api/items/{id}`    | `{"name": "...", "description": "...", "done": true}`  | Update an item |
| POST   | `/api/items/{id}/done` | -                                               | Mark an item done   |
| DELETE | `/api/items/{id}`    | -                                                 | Delete an item      |

## Testing with Postman

1. Import [`postman_collection.json`](./postman_collection.json) into Postman
   (Import → File → select the file). It includes all five requests above,
   pre-wired with a `baseUrl` collection variable set to
   `http://localhost:8080`.
2. Start the app (see above).
3. Run "Get all items" first to confirm the server responds, then try
   Create/Update/Delete.

## Testing with curl

```bash
# List
curl http://localhost:8080/api/items

# Get one
curl http://localhost:8080/api/items/1

# Create
curl -X POST http://localhost:8080/api/items \
  -H "Content-Type: application/json" \
  -d '{"name":"Buy groceries","description":"Milk, eggs, bread","done":false}'

# Update (use the id returned above)
curl -X PUT http://localhost:8080/api/items/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Buy groceries","description":"Milk, eggs, bread, butter","done":true}'

# Mark done
curl -X POST http://localhost:8080/api/items/1/done

# Delete
curl -X DELETE http://localhost:8080/api/items/1
```
