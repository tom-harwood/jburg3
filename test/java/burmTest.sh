SCRIPT_DIR=`dirname $0`
JBURG_HOME=$SCRIPT_DIR/../..

usage()
{
    echo "Usage: burmTest [-r] [-q] -g <grammar> -t <testcase> [-d <dumpfile>] [-p <templates>]"
}

args=`getopt c:d:g:m:p:t:rqv $*`
if [ $? -ne 0 ]
then
    usage;
    exit 2
fi

MAINCLASS=TestRunner

set -- $args

for i in $*
do
    case "$i"
    in
        -c)
            DUMPCLASS="-classname $2";
            shift;
            shift;;
        -m)
            MAINCLASS=$2;
            shift;
            shift;;
        -d)
            DUMPFILE="-dump $2";
            if [ "$TEMPLATES" == "" ]
            then
                TEMPLATES="-templates xml.stg"
            fi
            shift;
            shift;;
        -g)
            GRAMMAR=$2;
            shift;
            shift;;
        -p)
            TEMPLATES="-templates $2"
            shift;
            shift;;
        -q)
            QUIET="-quiet";
            shift;;
        -r)
            RANDOMIZE="-randomize";
            shift;;
        -t)
            TESTCASE=$2;
            shift;
            shift;;
        -v)
            VERBOSE="-verbose";
            shift;;
        --)
            break;;
    esac
done

java -ea -cp $JBURG_HOME/lib/jburg.jar:$HOME/tools/antlr-4.5.1-complete.jar:classes $MAINCLASS -grammar $GRAMMAR $QUIET $RANDOMIZE $VERBOSE $DUMPFILE $TEMPLATES $TESTCASE $DUMPCLASS
