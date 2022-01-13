#!/bin/sh

# save current dir
CUR_DIR=`pwd`

SCRIPT="$0"

# SCRIPT may be an arbitrarily deep series of symlinks. Loop until we have the concrete path.
while [ -h "$SCRIPT" ] ; do
  ls=`ls -ld "$SCRIPT"`
  # Drop everything prior to ->
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    SCRIPT="$link"
  else
    SCRIPT=`dirname "$SCRIPT"`/"$link"
  fi
done

# Read a value and fallback to a default value
readvalue () {
    read -p "$1 [$2]:" value
    if [ -z "$value" ]; then
        value=$2
    fi
    echo ${value}
}

increment_version ()
{
  declare -a part=( ${1//\./ } )
  declare    new
  declare -i carry=1

  for (( CNTR=${#part[@]}-1; CNTR>=0; CNTR-=1 )); do
    len=${#part[CNTR]}
    new=$((part[CNTR]+carry))
    [ ${#new} -gt ${len} ] && carry=1 || carry=0
    [ ${CNTR} -gt 0 ] && part[CNTR]=${new: -len} || part[CNTR]=${new}
  done

  new="${part[*]}"
  echo "${new// /.}"
}

promptyn () {
    while true; do
        read -p "$1 [Y]/N? " yn
        if [ -z "$yn" ]; then
            yn="y"
        fi
        case ${yn:-$2} in
            [Yy]* ) return 0;;
            [Nn]* ) return 1;;
            * ) echo "Please answer yes or no.";;
        esac
    done
}

run_integration_tests () {
    echo "Building the release..."
    mvn clean install -DskipTests -Prelease ${MAVEN_OPTION} >> /tmp/beyonder-${RELEASE_VERSION}.log

    if [ $? -ne 0 ]
    then
        tail -20 /tmp/beyonder-${RELEASE_VERSION}.log
        echo "Something went wrong. Full log available at /tmp/beyonder-$RELEASE_VERSION.log"
        exit 1
    fi
}

# determine beyonder home
FS_HOME=`dirname "$SCRIPT"`

# make FS_HOME absolute
FS_HOME=`cd "$FS_HOME"; pwd`

DRY_RUN=0

# Enter project dir
cd "$FS_HOME"

echo "Java version used:"
java --version
if promptyn "Do you want to use this version?"
then
    echo "Let's start the release process"
else
    echo "switch to another java version by using for example:"
    echo "sdk use java 10.0.2-oracle"
    exit 1
fi

CURRENT_BRANCH=`git rev-parse --abbrev-ref HEAD`
CURRENT_VERSION=`mvn help:evaluate -Dexpression=project.version|grep -Ev '(^\[|Download\w+:)'`

echo "Setting project version for branch $CURRENT_BRANCH. Current is $CURRENT_VERSION."
RELEASE_VERSION=$(readvalue "Enter the release version" ${CURRENT_VERSION%-SNAPSHOT})
NEXT_VERSION_P=`increment_version ${RELEASE_VERSION}`

NEXT_VERSION=$(readvalue "Enter the next snapshot version" ${NEXT_VERSION_P}-SNAPSHOT)

MAVEN_OPTION=$(readvalue "Enter any maven option you want to add" "")

RELEASE_BRANCH=release-${RELEASE_VERSION}

echo "STARTING LOGS FOR $RELEASE_VERSION..." > /tmp/beyonder-${RELEASE_VERSION}.log

# Check if the release already exists
git show-ref --tags | grep -q beyonder-${RELEASE_VERSION}
if [ $? -eq 0 ]
then
    if promptyn "Tag beyonder-$RELEASE_VERSION already exists. Do you want to remove it?"
    then
        git tag -d "beyonder-$RELEASE_VERSION"
    else
        echo "To remove it manually, run:"
        echo "tag -d beyonder-$RELEASE_VERSION"
        exit 1
    fi
fi

# Create a git branch
echo "Creating release branch $RELEASE_BRANCH..."
git branch | grep -q ${RELEASE_BRANCH}
if [ $? -eq 0 ]
then
    git branch -D ${RELEASE_BRANCH}
fi
git checkout -q -b ${RELEASE_BRANCH}

echo "Changing maven version to $RELEASE_VERSION..."
mvn versions:set -DnewVersion=${RELEASE_VERSION} >> /tmp/beyonder-${RELEASE_VERSION}.log

# Git commit release
git commit -q -a -m "prepare release beyonder-$RELEASE_VERSION"

# Testing that release will work at the end
run_integration_tests

# Just display the end of the build
tail -7 /tmp/beyonder-${RELEASE_VERSION}.log

# Tagging
echo "Tag version with beyonder-$RELEASE_VERSION"
git tag -a beyonder-${RELEASE_VERSION} -m "Release Elasticsearch Beyonder version $RELEASE_VERSION"
if [ $? -ne 0 ]
then
    tail -20 /tmp/beyonder-${RELEASE_VERSION}.log
    echo "Something went wrong. Full log available at /tmp/beyonder-$RELEASE_VERSION.log"
    exit 1
fi

# Preparing announcement
echo "Preparing announcement"
mvn changes:announcement-generate >> /tmp/beyonder-${RELEASE_VERSION}.log

echo "Check the announcement message"
cat target/announcement/announcement.vm

if promptyn "Is message ok?"
then
    echo "Message will be sent after the release"
else
    exit 1
fi

# Do we really want to publish artifacts?
RELEASE=0
if promptyn "Everything is ready and checked. Do you want to release now?"
then
    RELEASE=1
    # Deploying the version to final repository
    echo "Deploying artifacts to remote repository"
    if [ ${DRY_RUN} -eq 0 ]
    then
        mvn deploy -DskipTests -Prelease >> /tmp/beyonder-${RELEASE_VERSION}.log
        if [ $? -ne 0 ]
        then
            tail -20 /tmp/beyonder-${RELEASE_VERSION}.log
            echo "Something went wrong. Full log available at /tmp/beyonder-$RELEASE_VERSION.log"
            exit 1
        fi
    fi
fi

echo "Changing maven version to $NEXT_VERSION..."
mvn versions:set -DnewVersion=${NEXT_VERSION} >> /tmp/beyonder-${RELEASE_VERSION}.log
git commit -q -a -m "prepare for next development iteration"

# git checkout branch we started from
git checkout -q ${CURRENT_BRANCH}

if [ ${DRY_RUN} -eq 0 ]
then
    echo "Inspect Sonatype staging repositories"
    open https://s01.oss.sonatype.org/#stagingRepositories

    if promptyn "Is the staging repository ok?"
    then
        echo "releasing the nexus repository"
        mvn nexus-staging:release >> /tmp/beyonder-${RELEASE_VERSION}.log
    else
        echo "dropping the nexus repository"
        RELEASE=0
        mvn nexus-staging:drop >> /tmp/beyonder-${RELEASE_VERSION}.log
    fi
fi

# We are releasing, so let's merge into the original branch
if [ ${RELEASE} -eq 1 ]
then
    echo "Merging changes into ${CURRENT_BRANCH}"
    git merge -q ${RELEASE_BRANCH}
    git branch -q -d ${RELEASE_BRANCH}
    echo "Push changes to origin"
    if [ ${DRY_RUN} -eq 0 ]
    then
        git push origin ${CURRENT_BRANCH} beyonder-${RELEASE_VERSION}
        if promptyn "Do you want to announce the release?"
        then
            # We need to checkout the tag, announce and checkout the branch we started from
            git checkout -q beyonder-${RELEASE_VERSION}
            SMTP_USERNAME=$(readvalue "Enter your SMTP username" "david@pilato.fr")
            SMTP_PASSWORD=$(readvalue "Enter your SMTP password" "")
            mvn changes:announcement-mail -Dchanges.username=${SMTP_USERNAME} -Dchanges.password=${SMTP_PASSWORD} >> /tmp/beyonder-${RELEASE_VERSION}.log
            if [ $? -ne 0 ]
            then
                tail -20 /tmp/beyonder-${RELEASE_VERSION}.log
                echo "We have not been able to send the email. Full log available at /tmp/beyonder-$RELEASE_VERSION.log"
            fi
            git checkout -q ${CURRENT_BRANCH}
        else
            echo "Message not sent. You can send it manually using:"
            echo "mvn changes:announcement-mail"
        fi
    fi
else
    if promptyn "Do you want to remove $RELEASE_BRANCH branch and beyonder-${RELEASE_VERSION} tag?"
    then
        git branch -q -D ${RELEASE_BRANCH}
        git tag -d beyonder-${RELEASE_VERSION}
    fi
fi

# Go back in current dir
cd "$CUR_DIR"
