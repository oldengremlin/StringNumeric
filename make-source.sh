#!/bin/sh

CDIR=$( pwd )

SOURCENAME=source.code~StringNumeric.txt
cp /dev/null ${SOURCENAME}

echo "Вміст файлу source.txt:" > /dev/stderr
for F in $( find src/ -type f -name '*.java' -print ) $( find src/ -type f -name '*.xml' -print ) $( find src/ -type f -name '*.fxml' -print ) README.md; do
    if [ -r "${F}" ]; then
        echo "/*" >>${SOURCENAME}
        echo -n "• ${F} строки з "`wc -l ${SOURCENAME} | awk '{ printf("%s",$1) }'`" по " > /dev/stderr
        echo " * File: " ${F} >>${SOURCENAME}
        echo " */" >>${SOURCENAME}
        cat ${F} | sed "/^ *$/d" >>${SOURCENAME}
        echo `wc -l ${SOURCENAME} | awk '{ printf("%s",$1) }'` > /dev/stderr #'
        echo >>${SOURCENAME}
        sed -i "/^ *$/d" ${SOURCENAME}
    fi
done
