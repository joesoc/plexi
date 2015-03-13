# Commands to produce new releases #

**Note: These command sequences are not scripts.**

**Note: These command sequences are intended to be performed manually while being aware of what is happening and noticing errors and adjusting appropriately.**

**Note: Ensure you're using appropriate versions of JDK.**





### Create release branch ###
```
BRANCH=v4.0.x

# Make sure local code is up-to-date
git checkout master
git pull

# Create branch
git checkout -b $BRANCH
# Push branch for others to see
git push -u origin $BRANCH

#### In other local clones (optional) ####
# Get new branch
git fetch
# Create a local branch that pulls/pushes to origin/$BRANCH
git branch $BRANCH origin/$BRANCH
```

### Tag version ###
```
VERSION=0.90

cd pre-existing-plexi/
git checkout origin/master # the commit we are going to tag
git tag -a v$VERSION -m "Version $VERSION"
git push --tags
```

### Build RC of library ###
```
cd ~
git clone https://code.google.com/p/plexi/
cd plexi
git checkout v$VERSION
ant -Dadaptorlib.suffix=-$VERSION dist
```

### Extract documentation zip from full distribution zip ###
```
cd dist
unzip adaptor-$VERSION-bin.zip adaptor-$VERSION-docs.zip
```


### Build RC of adaptor ###
```
VERSION=0.90

# Tag version

# Have adaptor library
# eg: cd ~/plexi/dist
# unzip adaptor-$VERSION-bin.zip adaptor-$VERSION-withlib.jar

# Build adaptor from fresh clone
cd ~
git clone https://code.google.com/p/plexi.repo/
cd plexi.repo
git checkout v$VERSION
cp ../plexi/dist/adaptor-$VERSION-withlib.jar lib/
ant -Dadaptor.suffix=-$VERSION -Dadaptor.jar=lib/adaptor-$VERSION-withlib.jar dist

```





<a href='Hidden comment: 
Scratch padgit checkout v4.0.x
git merge $sha1beginning # for fstfwd
git cherrypick master # commit to add
git cherry v4.0.x # information to cmp checkout to this one
    # "+" we have it; target doesn"t
    # "-" we both have it
git push v4.0.x:4.0.x # refspec example
git checkout -b mybranchname
git merge
git branch -D mybranch
git checkout master^^^^^
</wiki:comment>


<wiki:comment>
   NO LONGER DOING THE DOCUMENTATION THIS WAY

=== Update online documentation ===
{{{
cd ~
git clone https://code.google.com/p/plexi.documentation/
cd plexi.documentation
rm -r *
mkdir javadoc && cd javadoc
unzip ~/plexi/dist/adaptor-$VERSION-docs.zip
git add -A
git commit -m "Update for version $VERSION"
git tag -a v$VERSION -m "Version $VERSION"
git push --tags origin master

# Update "External links" to point to new documentation.
# Update main page to point to new release.
}}}
</wiki:comment>```