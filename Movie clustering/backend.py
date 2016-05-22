import pandas as pd
import nltk
import re
from sklearn.cluster import KMeans
from sklearn.decomposition import PCA
from sklearn.manifold import MDS
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.feature_extraction.text import TfidfVectorizer

titles = []
synopses = []


def read_from_file():
    with open('movie_synopsis.txt', 'r') as f1:
        for l in f1:
            k, v = l.strip().split(":::")
            titles.append(k)
            synopses.append(v)

read_from_file()


def tokenize_and_stem(text):
    from nltk.stem.snowball import SnowballStemmer
    stemmer = SnowballStemmer("english")
    tokens = [word for sent in nltk.sent_tokenize(
        text) for word in nltk.word_tokenize(sent)]
    filtered_tokens = []
    # filter out any tokens not containing letters (e.g., numeric tokens, raw
    # punctuation)
    for token in tokens:
        if re.search('[a-zA-Z]', token):
            filtered_tokens.append(token)
    stems = [stemmer.stem(t) for t in filtered_tokens]
    return stems

totalvocab_stemmed = []

for i in synopses:
    allwords_stemmed = tokenize_and_stem(i)
    totalvocab_stemmed.extend(allwords_stemmed)

vocab_frame = pd.DataFrame(
    {'words': totalvocab_stemmed}, index=totalvocab_stemmed)

# define vectorizer parameters
tfidf_vectorizer = TfidfVectorizer(max_df=0.8, max_features=200000,
                                   min_df=0.01, stop_words='english',
                                   use_idf=True, ngram_range=(1, 1), analyzer='word')

tfidf_matrix = tfidf_vectorizer.fit_transform(synopses)
terms = tfidf_vectorizer.get_feature_names()
# print(terms[443])


def clear(tfidf_matrix=tfidf_matrix):
    # global tfidf_matrix
    tfidf_matrix = tfidf_vectorizer.fit_transform(synopses)
    return get_initial_coordinates(tfidf_matrix)


def get_initial_coordinates(tfidf_matrix=tfidf_matrix):
    # fit the vectorizer to synopses
    num_clusters = 4

    km = KMeans(n_clusters=num_clusters, random_state=1)

    km.fit(tfidf_matrix)

    clusters = km.labels_.tolist()

    # print(tfidf_matrix)

    films = {'title': titles, 'synopsis': synopses, 'cluster': clusters}

    frame = pd.DataFrame(films, index=[clusters], columns=['title', 'cluster'])

    print(tfidf_matrix)

    dist = 1 - cosine_similarity(tfidf_matrix)
    # print(dist)

    # groupby cluster for aggregation purposes
    # grouped = frame.groupby(['cluster'])

    # print("Top terms per cluster:")
    # sort cluster centers by proximity to centroid
    # cluster_centers_ coordinates of cluster centers.
    # Since it is normalised the highest val dim is contributing most to
    # centroid
    cluster_centers = km.cluster_centers_
    order_centroids = cluster_centers.argsort()[:, ::-1]
    # order_centroids_vals = km.cluster_centers_.sort()[:, ::-1]
    # print('Km cluster centers')
    # print(km.cluster_centers_)
    # print('km.cluster_centers_.argsort()')
    # print(km.cluster_centers_.argsort())
    # print('km.cluster_centers_.argsort()[:, ::-1]')
    # print(order_centroids)
    # print(len(terms))
    # print(km.labels_.tolist())

    word_cloud = []
    for i in range(num_clusters):
        print("")
        print("cluster number " + str(i))
        for ind in order_centroids[i, :6]:
            temp = {}
            temp['cluster'] = i
            temp['text'] = terms[ind]
            temp['size'] = cluster_centers[i][ind]
            word_cloud.append(temp)
            print(' %s ' % terms[ind])
            # print(' %f' % cluster_centers[i][ind])

        # for ind in order_centroids_vals[i, :6]:
        #     print(' %d' % terms[ind])

    pca = PCA(n_components=2)

    pos = pca.fit_transform(dist)  # shape (n_components, n_samples)

    # mds = MDS(n_components=2, dissimilarity="precomputed", random_state=1)
    # pos = mds.fit_transform(dist)
    # print(dist)

    xs, ys = pos[:, 0], pos[:, 1]
    print((xs, ys))
    print(word_cloud)

    return (xs, ys, titles, synopses, clusters, word_cloud)


def get_distance(tfidf_matrix=tfidf_matrix):

    num_clusters = 3

    km = KMeans(n_clusters=num_clusters)

    km.fit(tfidf_matrix)

    clusters = km.labels_.tolist()

    dist = 1 - cosine_similarity(tfidf_matrix)
    return dist, titles, synopses, clusters


def update_matrix_el(x, y, tfidf_matrix=tfidf_matrix):
    percent = 0.2 * tfidf_matrix[x, y]
    dist_from_one = (1 - tfidf_matrix[x, y]) * 0.2
    tfidf_matrix[x, y] += min(percent, dist_from_one)
    # if tfidf_matrix[x, y] > 0.5:
    #     tfidf_matrix[x, y] = tfidf_matrix[x, y] + ((1 - tfidf_matrix[x, y])/2)
    # else:
    #     tfidf_matrix[x, y] = tfidf_matrix[x, y] * 2


def update_tfidf(words, tfidf_matrix=tfidf_matrix):
    words = words.split("_")
    print(words)
    index_set = set()
    for w in words:
        for i, t in enumerate(terms):
            if w in t:
                print(terms[i])
                index_set.add(i)

    x_ind, y_ind = tfidf_matrix.nonzero()
    y_ind_set = set(y_ind)

    common_elements = index_set.intersection(y_ind_set)
    common_index = [i for i, val in enumerate(y_ind) if val in common_elements]
    print(common_elements)
    print(common_index)
    for i in common_index:
        update_matrix_el(x_ind[i], y_ind[i])

    # print(tfidf_matrix)

# update_tfidf("life")
get_initial_coordinates()
