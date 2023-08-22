# Minecraft Bedrock Server Status Web App

This project provides a Flask web application that displays information about a Minecraft Bedrock server. The application fetches server information using an external API and presents it in a user-friendly format on a web page.

![Screenshot of the Web Application](screenshot.png)

## Quick setup

   ```bash
   docker run -d -p 5000:5000 -e SERVER=<YOUR-MINECRAFT_SERVER_DOMAIN_HERE> rdnsx/bedrockstatus
   ```

Access the application by opening your web browser and navigating to `http://localhost:5000`.

## Setup with Docker-Compose

1. Clone this repository to your local machine:

   ```bash
   git clone https://github.com/rdnsx/Bedrock-Status.git
   ```

2. Navigate to the project directory:

   ```bash
   cd Bedrock-Status
   ```

3. Modify the `docker-compose.yml` file to set your desired environment variables. Specifically, you can set the `SERVER` environment variable to the domain of the Minecraft Bedrock server you want to monitor.

   ```yaml
   services:
     bedrockstatus:
       image: rdnsx/bedrockstatus
       ports:
         5000:5000
       environment:
         - SERVER=<YOUR-MINECRAFT_SERVER_DOMAIN_HERE>
   ```

4. Deploy the application using Docker Compose:

   ```bash
   docker-compose up -d
   ```

5. Access the application by opening your web browser and navigating to `http://localhost:5000`.

## Customization

- You can modify the Flask application's code in the `app.py` file to enhance or change the way server information is presented on the web page.
- CSS, HTML and images for the web page are located in the `templates` and `static` directories. You can customize these files to change the appearance and layout of the web page.

## Notes

- The application fetches server information from an external API. If you encounter any issues with the API, ensure that the API URL (`https://api.mcsrvstat.us/bedrock/2/<YOU_DOMAIN>`) is still valid and working.
- This project uses the Docker image `rdnsx/bedrockstatus` to deploy the application. Make sure to check for any updates or changes to this image if needed.

## License

This project is licensed under the [MIT License](LICENSE).