# -*- coding: utf-8 -*-

import json
import time
import urllib
from urllib2 import Request, urlopen


class APIBase(object):
    """The Base class for making API calls
            All the new urls to be called will be a function here
            Each of these functions will call _call_api
    """

    def __init__(self):
        self.base_url = 'https://api.themoviedb.org/3'
        self.full_url = ''
        self.params = {'api_key': '9bf53c5c13628f9b4bd7cc498bbf61ab'}
        self.headers = {'Accept': 'application/json'}

    def _call_api(self, api_url, params=None):
        if params:
            self.params.update(params)
        self.full_url = self.base_url + api_url + \
            '?' + urllib.urlencode(self.params)
        print(self.full_url)
        request = Request(self.full_url, headers=self.headers)
        response_body = urlopen(request).read()
        return json.loads(response_body)

    def search_movie_by_title(self, title, page=1):
        return self._call_api('/search/movie', {'query': title, 'page': page})

    def find_similar_movies(self, movie_id):
        return self._call_api('/movie/{0}/similar'.format(movie_id))

    def movie_synopsis(self, page=1):
        return self._call_api('/movie/top_rated', {'page': page})


def get_movie_synopsis():
    f1 = open('./movie_synopsis.txt', 'w+')
    for i in range(1, 10):
        synopsis_list = APIBase().movie_synopsis(i)
        for val in synopsis_list['results']:
            try:
                f1.write(str(val['title']) + ':::' + str(val['overview']) + '\n')
            except Exception:
                pass
            



def q1b():
    count = 0
    loop_count = 1
    is_break = False
    movie_id_list = []
    f1 = open('./​​movie_ID_name.txt', 'w+')
    # h = HTMLParser.HTMLParser()
    while True:
        life_movies = APIBase().search_movie_by_title('life', loop_count)
        for val in life_movies['results']:
            # Unicode to string conversion
            title_string = val['title']
            # The string life might be in original_title etc
            if 'life' not in title_string.lower():
                continue
            # CGI unescape
            f1.write(
                str(val['id']) + ', ' + title_string.encode('utf-8') + '\n')
            movie_id_list.append(val['id'])
            count = count + 1
            print (count)
            if count == 300:
                is_break = True
                break
        if is_break:
            break
        loop_count = loop_count + 1
    print ("Finished writing to file")
    return movie_id_list


def q1c(movie_list):
    ans = []
    f2 = open('./movie_ID_sim_movie_ID.txt', 'w+')
    for i, movie_id in enumerate(movie_list):
        if not i % 39:
            sleep_wrap(10)
            print ('Populated similar movie tuples for ' + str(i) + ' movies')
        life_movies = APIBase().find_similar_movies(movie_id)
        count = 0
        for val in life_movies['results']:
            # Add the key,val
            # if val, key is already present in ans
            # 	=> remove the one with bigger first element
            ans.append((movie_id, val['id']))
            if movie_id < val['id']:
                if ((val['id'], movie_id) in ans):
                    ans.remove((val['id'], movie_id))
            else:
                if ((val['id'], movie_id) in ans):
                    ans.remove((movie_id, val['id']))
            count = count + 1
            if count == 5:
                break
    print ('Writing to the file movie_ID_sim_movie_ID.txt')

    for key, (v1, v2) in enumerate(ans):
        f2.write(str(v1) + ', ' + str(v2) + '\n')


def sleep_wrap(n):
    print ('Start sleep for ' + str(n) + ' seconds')
    time.sleep(n)
    print('Finish sleep')

get_movie_synopsis()
# movie_id_list = q1b()
# q1c(movie_id_list)
