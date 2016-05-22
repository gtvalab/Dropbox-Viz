import numpy as np
import pandas as pd
import nltk
import re
import os
import codecs
from sklearn.manifold import MDS
from sklearn.cluster import KMeans
from sklearn import feature_extraction
from sklearn.decomposition import PCA
from sklearn.metrics.pairwise import cosine_similarity, euclidean_distances
from sklearn.feature_extraction.text import TfidfVectorizer

movie_synopsis = {}
titles = []
synopses = []


def read_from_file():
    with open('movie_synopsis.txt', 'r') as f1:
        for l in f1:
            k, v = l.strip().split(":::")
            movie_synopsis[k] = v
            titles.append(k)
            synopses.append(v)

read_from_file()


def tokenize_and_stem(text):
    from nltk.stem.snowball import SnowballStemmer
    stemmer = SnowballStemmer("english")
    tokens = [word for sent in nltk.sent_tokenize(text) for word in nltk.word_tokenize(sent)]
    filtered_tokens = []
    # filter out any tokens not containing letters (e.g., numeric tokens, raw
    # punctuation)
    for token in tokens:
        if re.search('[a-zA-Z]', token):
            filtered_tokens.append(token)
    stems = [stemmer.stem(t) for t in filtered_tokens]
    return stems


# def tokenize_only(text):
# first tokenize by sentence, then by word to ensure that punctuation is
# caught as it's own token
#     tokens = [word.lower() for sent in nltk.sent_tokenize(text)
#               for word in nltk.word_tokenize(sent)]
#     filtered_tokens = []
# filter out any tokens not containing letters (e.g., numeric tokens, raw
# punctuation)
#     for token in tokens:
#         if re.search('[a-zA-Z]', token):
#             filtered_tokens.append(token)
#     return filtered_tokens

totalvocab_stemmed = []
# totalvocab_tokenized = []

for i in synopses:
    allwords_stemmed = tokenize_and_stem(i)
    totalvocab_stemmed.extend(allwords_stemmed)

    # allwords_tokenized = tokenize_only(i)
    # totalvocab_tokenized.extend(allwords_tokenized)

vocab_frame = pd.DataFrame({'words': totalvocab_stemmed}, index=totalvocab_stemmed)

# define vectorizer parameters
tfidf_vectorizer = TfidfVectorizer(max_df=0.8, max_features=200000,
                                   min_df=0.01, stop_words='english',
                                   use_idf=True, tokenizer=tokenize_and_stem, ngram_range=(1, 3), analyzer='word')

tfidf_matrix = tfidf_vectorizer.fit_transform(synopses)

def get_initial_coordinates(tfidf_matrix=tfidf_matrix):
    # fit the vectorizer to synopses
    terms = tfidf_vectorizer.get_feature_names()
    # print(terms)

    num_clusters = 4

    km = KMeans(n_clusters=num_clusters)

    km.fit(tfidf_matrix)

    clusters = km.labels_.tolist()

    # print(clusters)

    films = {'title': titles, 'synopsis': synopses, 'cluster': clusters}

    frame = pd.DataFrame(films, index=[clusters], columns=['title', 'cluster'])

    print(tfidf_matrix)

    dist = 1 - cosine_similarity(tfidf_matrix)
    print(dist)

    # groupby cluster for aggregation purposes
    # grouped = frame.groupby(['cluster'])

    print("Top terms per cluster:")
    # sort cluster centers by proximity to centroid
    order_centroids = km.cluster_centers_.argsort()[:, ::-1]
    print('Km cluster centers')
    print(km.cluster_centers_)
    print('km.cluster_centers_.argsort()')
    print(km.cluster_centers_.argsort())
    print('km.cluster_centers_.argsort()[:, ::-1]')
    print(len(order_centroids[0]))
    print(len(terms))

    for i in range(num_clusters):
        print("Cluster %d words:" % i)

        for ind in order_centroids[i, :6]:  # replace 6 with n words per cluster
            # print(' %s' % vocab_frame.ix[terms[ind].split(' ')].values.tolist()[0][0].encode('utf-8', 'ignore'))
            print(' %s' % terms[ind])
            # print("Cluster %d titles:" % i, end='')
            # for title in frame.ix[i]['title'].values.tolist():
            #     print(' %s,' % title)
            #     print()  # add whitespace

    pca = PCA(n_components=2)

    pos = pca.fit_transform(dist)  # shape (n_components, n_samples)

    xs, ys = pos[:, 0], pos[:, 1]

    return (xs, ys, titles, synopses, clusters)

    # print(xs)
    # print(ys)
