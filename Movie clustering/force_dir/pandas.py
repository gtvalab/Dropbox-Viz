import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

if __name__ == '__main__':
    rating_datafile = 'data/u.data'
    user_datafile = 'data/u.user'
    movie_datafile = 'data/u.item'

    outfile = open('movies.txt', 'w')

    rating_data = [tuple(rating.split('\t'))
                   for rating in open(rating_datafile).read().split('\n')[:-1]]
    user_data = [tuple(user.split('|'))
                 for user in open(user_datafile).read().split('\n')[:-1]]
    movie_data = [tuple(film.split('|'))
                  for film in open(movie_datafile).read().split('\n')[:-1]]

    rating_dataframe = pd.DataFrame(
        data=rating_data, columns=['user_id', 'item_id', 'rating', 'timestamp'])
    user_dataframe = pd.DataFrame(
        data=user_data, columns=['user_id', 'age', 'gender', 'occupation', 'zip_code'])
    movie_dataframe = pd.DataFrame(data=movie_data, columns=['movie_id', 'movie_title', 'release_date', 'video_release_date', 'imdb_url', 'unknown', 'action', 'adventure',
                                                             'animation', 'childrens', 'comedy', 'crime', 'documentary', 'drama', 'fantasy', 'noir', 'horror', 'musical', 'mystery', 'romance', 'scifi', 'thriller', 'war', 'western'])

    movie_count = movie_dataframe.describe()['movie_id']['count']
    user_count = user_dataframe.describe()['user_id']['count']

    # TODO: top 5 movies with the most ratings
    # output format = movie_name\tratings
    top_5 = rating_dataframe['item_id'].value_counts()[0:4]
    outfile.write(str(top_5))

    # TODO: top 5 movies with the lowest average age of the people that rated the movies, considering only films with at least 100 ratings
    # output format = movie_name\tnumber of ratings\t avg_age

    frequent_results = rating_dataframe.groupby(
        'item_id').filter(lambda x: len(x) > 100)
    ratings_over_100 = frequent_results
    #    ratings_over_100 = rating_dataframe[rating_dataframe['item_id'].value_counts()>100]

    selected_user_frame = pd.merge(
        ratings_over_100, user_dataframe, on='user_id', how='left')
    selected_user_frame['age'] = selected_user_frame[
        'age'].apply(lambda x: int(x))
    foo = selected_user_frame.groupby(['item_id'])['age'].mean()
    foobar = selected_user_frame.groupby(['item_id'])['user_id'].size()
    bar = []

    for i in list(set(selected_user_frame['item_id'])):
        movie_title = movie_dataframe[
            movie_dataframe['movie_id'] == i]['movie_title']
        movie_title = ' '.join(str(movie_title).split('\n')[0].split(' ')[4:])
        bar.append((movie_title, foobar[i], foo[i]))
    baz = sorted(bar, key=lambda x: x[-1])

    for b in baz[:5]:
        outfile.write('%s\t%d\t%f' % b)

    # TODO: scatterplot with marginal histograms- visualize how average rating
    # varies with age (scatterhist.png) y is rating, x is age
    fig, ax = plt.subplots()

    merged_rating_frame = pd.merge(
        rating_dataframe, user_dataframe, on='user_id', how='left')

    merged_rating_frame['age'] = merged_rating_frame[
        'age'].apply(lambda x: int(x))

    merged_rating_frame['rating'] = merged_rating_frame[
        'rating'].apply(lambda x: int(x))

    sns.jointplot(x='age', y='rating', data=merged_rating_frame)

    # TODO generate density estimates for scatterplot above, save as
    # scatterbonus.png
    plt.show()

    sns.jointplot(x='age', y='rating', data=dataframe, kind='kde', ax=ax)
    plt.show()
