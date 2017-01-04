# akka-http-batch-api

Sample Implementation of a batch api using Akka-Http

Uses `argonaut` for converting to and from JSON.

Sample Usage:

Without batch api - normal request:

```
$ curl -X POST -H 'Content-Type: application/json' http://localhost:9000/movie -d '{"t": "Inception", "plot": "short" }' | jq

{
  "Title": "Inception",
  "Year": "2010",
  "Director": "Christopher Nolan",
  "Plot": "A thief, who steals corporate secrets through use of dream-sharing technology, is given the inverse task of planting an idea into the mind of a CEO.",
  "Response": "True",
  "imdbRating": "8.8",
  "Released": "16 Jul 2010"
}
```

**Note:** `jq` is used to pretty print the json response 

With batch api:

```
$ curl -X POST -H 'Content-Type: application/json' http://localhost:9000/batch -d '[{"method": "POST", "relative_url": "/movie", "body": "{\n\t\"t\": \"Inception\",\n\t\"plot\": \"short\"}"},{"method": "POST", "relative_url": "/movie", "body": "{\n\t\"t\": \"The Dark Knight\"}"}]' | jq

[
  {
    "code": 200,
    "headers": [],
    "body": "{\"Title\":\"Inception\",\"Year\":\"2010\",\"Director\":\"Christopher Nolan\",\"Plot\":\"A thief, who steals corporate secrets through use of dream-sharing technology, is given the inverse task of planting an idea into the mind of a CEO.\",\"Response\":\"True\",\"imdbRating\":\"8.8\",\"Released\":\"16 Jul 2010\"}"
  },
  {
    "code": 200,
    "headers": [],
    "body": "{\"Title\":\"The Dark Knight\",\"Year\":\"2008\",\"Director\":\"Christopher Nolan\",\"Plot\":\"When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, the caped crusader must come to terms with one of the greatest psychological tests of his ability to fight injustice.\",\"Response\":\"True\",\"imdbRating\":\"9.0\",\"Released\":\"18 Jul 2008\"}"
  }
]
```
