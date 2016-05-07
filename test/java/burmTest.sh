SCRIPT_DIR=`dirname $0`
JBURG_HOME=$SCRIPT_DIR/../..

usage()
{
    echo "Usage: burmTest -g <grammar> -t <testcase>"
}

args=`getopt g:t:r $*`
if [ $? -ne 0 ]
then
    usage;
    exit 2
fi

set -- $args

for i in $*
do
    case "$i"
    in
        -g)
            GRAMMAR=$2;
            shift;
            shift;;
        -r)
            RANDOMIZE="-randomize";
            shift;;
        -t)
            TESTCASE=$2;
            shift;
            shift;;
        --)
            break;;
    esac
done

if [ "$GRAMMAR" = "" -o "$TESTCASE" = "" ]
then
    echo You must specify -grammar and -testcase.
    usage;
    exit 2
fi

java -cp $JBURG_HOME/lib/jburg.jar:classes Calculator -grammar $GRAMMAR $RANDOMIZE $TESTCASE
