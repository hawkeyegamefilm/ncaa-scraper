# ncaa-scraper
Groovy scraper designed to scrape and parse NCAA games from the offical NCAA site.


This project is designed to hit the JSON API provided by the NCAA public site and convert it into csv files that can be bulk loaded.


Play.csv target format:
"Game Code","Play Number","Period Number","Clock","Offense Team Code","Defense Team Code","Offense Points","Defense Points","Down","Distance","Spot","Play Type","Drive Number","Drive Play"

Driver.csv target format:
"Game Code","Drive Number","Team Code","Start Period","Start Clock","Start Spot","Start Reason","End Period","End Clock","End Spot","End Reason","Plays","Yards","Time Of Possession","Red Zone Attempt"
