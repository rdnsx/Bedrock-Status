from flask import Flask, render_template
import requests
import os 

app = Flask(__name__)

MC_SERVER_DOMAIN = os.environ.get('SERVER')

def get_minecraft_server_status():
    try:
        # First try for the Java version
        response = requests.get(f'https://api.mcsrvstat.us/3/{MC_SERVER_DOMAIN}')
        data = response.json()
        if data['online']:
            # Construct and return data for Java version
            return construct_server_data(data)
    except Exception as e:
        print(f"Java version error: {e}")
        try:
            # Failover to the Bedrock version
            response = requests.get(f'https://api.mcsrvstat.us/bedrock/2/{MC_SERVER_DOMAIN}')
            data = response.json()
            if data['online']:
                # Construct and return data for Bedrock version
                return construct_server_data(data)
        except Exception as e:
            print(f"Bedrock version error: {e}")

    return None

def construct_server_data(data):
    return {
        'server_version': data.get('version', 'Unknown'),
        'motd': data['motd']['clean'][0] if 'motd' in data and 'clean' in data['motd'] else 'Unknown',
        'max_players': data['players'].get('max', 0),
        'connected_players': data['players'].get('online', 0),
        'hostname': data.get('hostname', 'Unknown'),
        'map': data.get('map', 'Unknown'),
        'gamemode': data.get('gamemode', 'Unknown'),
        'server_id': data.get('serverid', 'Unknown'),
        'online_state': True
    }

@app.route('/')
def index():
    server_info = get_minecraft_server_status()
    return render_template('index.html', server_info=server_info)

if __name__ == '__main__':
    app.run(debug=True)
