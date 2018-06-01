# Hadoop-Inverted-Index

Environment
---
JavaSE-1.8, hadoop-2.9.0

Instruction
---
hadoop jar {path}/invertedindex-0.0.1-SNAPSHOT.jar edu.ucr.cs.cs242.InvertedIndex [input file] [output dir] [number of posts]   
hadoop jar /home/ben/invertedindex-0.0.1-SNAPSHOT.jar edu.ucr.cs.cs242.InvertedIndex transformed_data.txt invertedIndex 496  
  
*[number of posts] is from the output file of preprocess (numPosts.txt)

Format
---
Input:  
data after preprocessing (transformed_data.txt)  
  
Output:  
< key | value > == < word | {[positions of word] ,#document id, tfidf score} …>   
*ordered by score, each row is a Json object

Reference document id to URL by (ref_data.txt) from preprocessing
