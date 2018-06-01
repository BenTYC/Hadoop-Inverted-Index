# Hadoop-Inverted-Index

Environment
---
JavaSE-1.8, hadoop-2.9.0

Instruction
---
hadoop jar invertedindex-0.0.1-SNAPSHOT.jar edu.ucr.cs.cs242.InvertedIndex [input file] [output dir] [number of posts] 
* [number of posts] is from the output file of preprocess (numPosts.txt)

Format
---
Input:  
data after preprocessing (transformed_data.txt)  
  
Output:  
< key | value > == < word | {[positions of word] ,#document id, tfidf score} ...>   
* ordered by score, each row is a Json object

Reference document id to URL by (ref_data.txt) from preprocessing


Details of how hadoop was used (key, values definitions and data flow)
---
![MapReduce](https://github.com/BenTYC/Hadoop-Inverted-Index/blob/master/MR.png "MapReduce")

Above shows how our MapReduce indexing work. The mapper deal with the information of each word, like document id and position, then the combiner combine this information from the same post and calculate the term frequency of each word. Finally, the reducer sum up this information of the same word. The output is the inverted index and tf-idf score for each word and its format is Json.The result is already ordered by their tf-idf score.
![index](https://github.com/BenTYC/Hadoop-Inverted-Index/blob/master/index.png =50x50)

Stem the keywords
---
![stemming](https://github.com/BenTYC/Hadoop-Inverted-Index/blob/master/stem.png)
<img src="https://github.com/BenTYC/Hadoop-Inverted-Index/blob/master/stem.png" width="48">

As figure above showed, these four types of word are derived from the same origin, so we use the stemming algorithm and store only one word in index. We use porter stemming algorithm from this website ( https://tartarus.org/martin/PorterStemmer/ ). It provides API for many popular languages.
