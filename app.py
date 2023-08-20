from flask import Flask, render_template
import requests

app = Flask(__name__)

def get_minecraft_server_status():
    try:
        response = requests.get('https://api.mcsrvstat.us/bedrock/2/pietscraft.net')
        data = response.json()
        if data['online']:
            return {
                'server_version': data['version'],
                'motd': data['motd']['clean'][0],
                'max_players': data['players']['max'],
                'connected_players': data['players']['online'],
                'hostname': data['hostname'],
                'map': data['map'],
                'gamemode': data['gamemode'],
                'server_id': data['serverid'],
                'online_state': True
            }
    except Exception as e:
        print(f"Error: {e}")
    return None

@app.route('/')
def index():
    server_info = get_minecraft_server_status()
    return render_template('index.html', server_info=server_info)

if __name__ == '__main__':
    app.run(debug=True)
