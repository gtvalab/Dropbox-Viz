from bottle import route, run, hook, response, request, redirect
import json
import dropbox
import urllib.request as req
from dropbox.client import DropboxOAuth2Flow, DropboxClient

# Get your app key and secret from the Dropbox developer website
app_key = '9ero453euch91e0'
app_secret = '621050byozk9lgd'
dropbox_redirect_uri = "http://localhost:8080/redirect_uri"
session = {}
dropbox_flow = None
client = None

# flow = dropbox.client.DropboxOAuth2FlowNoRedirect(app_key, app_secret)
# authorize_url = flow.start()
# print ('1. Go to: ' + authorize_url)
# print ('2. Click "Allow" (you might have to log in first)')
# print ('3. Copy the authorization code.')

# code = input("Enter the authorization code here: ").strip()
# access_token, user_id = flow.finish(code)
# client = dropbox.client.DropboxClient(access_token)


@hook('after_request')
def enable_cors():
    """
    You need to add some headers to each request.
    Don't use the wildcard '*' for Access-Control-Allow-Origin in production.
    """
    response.headers['Access-Control-Allow-Origin'] = '*'
    response.headers[
        'Access-Control-Allow-Methods'] = 'PUT, GET, POST, DELETE, OPTIONS'
    response.headers[
        'Access-Control-Allow-Headers'] = 'Origin, Accept, Content-Type, X-Requested-With, X-CSRF-Token'

@route('/dropbox-auth-start')
def dropbox_auth_start():
	new_url = get_auth_flow().start()
	return redirect(new_url)

def get_auth_flow():
    return DropboxOAuth2Flow(app_key, app_secret, dropbox_redirect_uri, session, 'dropbox-auth-csrf-token')


@route('/redirect_uri')
def redirect_uri():
	# print(request.url_args)
	dropbox_dict = {}
	dropbox_dict['state'] = request.query.state
	dropbox_dict['code'] = request.query.code
	print(dropbox_dict)
	access_token, user_id, url_state = get_auth_flow().finish(request.query)
	global client
	client = dropbox.client.DropboxClient(access_token)
	return(client.account_info())
	# return dropbox_code

@route('/dropbox-auth-finish')
def finish_auth():
	access_token, user_id, url_state = get_auth_flow().finish(request.query)

@route('/metadata/:folder_path')
def get_metadata(folder_path):
	path = '/' + folder_path
	return client.metadata(path)


@route('/test')
def initial_corordinates():
	return (client.account_info())
	# return("test")

run(host='localhost', port=8080, debug=True)