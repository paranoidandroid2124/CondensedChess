import requests
import json

url = "https://chesstory-api-145435701096.us-central1.run.app/auth/login"
payload = {"email": "test@test.com", "password": "test"}
headers = {"Content-Type": "application/json"}

try:
    print(f"Sending POST to {url}...")
    response = requests.post(url, json=payload, headers=headers)
    print(f"Status Code: {response.status_code}")
    print(f"Response Body: {response.text}")
    try:
        print(f"Parsed JSON: {response.json()}")
    except:
        print("Response is NOT valid JSON")
except Exception as e:
    print(f"An error occurred: {e}")
