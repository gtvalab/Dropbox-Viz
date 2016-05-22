from bottle import route, run, hook, response
import backend
import json


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

# Scatter plot
@route('/initial_corordinates')
def initial_corordinates():
    (xs, ys, titles, synopses, clusters, word_cloud) = backend.get_initial_coordinates()
    l = min(len(xs), len(ys), len(titles), len(clusters))
    final = {}
    ans = []
    for i in range(0, l):
        temp = {}
        temp['name'] = titles[i]
        temp['cluster'] = clusters[i]
        temp['synopsis'] = synopses[i]
        temp['x'] = xs[i]
        temp['y'] = ys[i]
        ans.append(temp)
    final['coordinates'] = ans
    final['word_cloud'] = word_cloud
    return json.dumps(final)


# Force Directed
@route('/get_distance')
def get_distance():
    (dist, titles, synopses, clusters) = backend.get_distance()
    print(dist[0])
    print(len(dist))
    l = min(len(synopses), len(titles), len(clusters))
    nodes = []
    links = []
    print(l)
    for i in range(0, l):
        temp = {}
        temp['name'] = titles[i]
        temp['group'] = clusters[i]
        # temp['synopsis'] = synopses[i]
        nodes.append(temp)
    min_val = 1
    for i in range(0, l):
        for j in range(0, l):
            if dist[i][j] >= 0.1 and min_val > dist[i][j]:
                min_val = dist[i][j]
    print(min_val)
    for i in range(0, l):
        for j in range(0, l):
            if i == j or dist[i][j] == 1:
                continue
            temp = {}
            temp['source'] = i
            temp['target'] = j
            temp['value'] = (dist[i][j] - min_val)/(1 - min_val)
            links.append(temp)

    final = {}
    final['nodes'] = nodes
    final['links'] = links
    return json.dumps(final)


@route('/update/<words>')
def update_matrix(words):
    backend.update_tfidf(words)
    print(words)
    return words


@route('/clear')
def clear():
    backend.clear()
    return None


run(host='localhost', port=8083)