SCRIPT_DIR=`dirname $0`
JBURG_HOME=$SCRIPT_DIR/../..

usage()
{
    echo "Usage: burmTest [-r] [-q] -g <grammar> -t <testcase>"
}

args=`getopt c:d:g:t:rq $*`
if [ $? -ne 0 ]
then
    usage;
    exit 2
fi

MAINCLASS=Calculator

set -- $args

for i in $*
do
    case "$i"
    in
        -c)
            MAINCLASS=$2;
            shift;
            shift;;
        -d)
            DUMPFILE="-dump $2";
            shift;
            shift;;
        -g)
            GRAMMAR=$2;
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
        --)
            break;;
    esac
done

if [ "$MAINCLASS" = "Calculator" ]
then
    if [ "$GRAMMAR" = "" -o "$TESTCASE" = "" ]
    then
        echo You must specify -grammar and -testcase.
        usage;
        exit 2
    fi
fi

java -cp $JBURG_HOME/lib/jburg.jar:$HOME/tools/antlr-4.5.1-complete.jar:classes $MAINCLASS -grammar $GRAMMAR $QUIET $RANDOMIZE $DUMPFILE $TESTCASE
