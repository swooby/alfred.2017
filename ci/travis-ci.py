#!/usr/bin/python

"""
Uploads an apk to Google Play

Requirements:
sudo -H easy_install --upgrade requests
sudo -H easy_install --upgrade google-api-python-client

Derived from:
 https://github.com/googlesamples/android-play-publisher-api/blob/master/v2/python/basic_list_apks_service_account.py

Reference:
 https://developers.google.com/android-publisher/
 https://developers.google.com/android-publisher/api-ref/
 https://developers.google.com/resources/api-libraries/documentation/androidpublisher/v2/python/latest/
 https://developers.google.com/android-publisher/libraries
 https://github.com/googlesamples/android-play-publisher-api/tree/master/v2/python

Client Source:
 https://github.com/google/google-api-python-client/tree/master/googleapiclient
"""

import os
import requests
import subprocess
import sys
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload
from oauth2client.service_account import ServiceAccountCredentials


def call(args):
    returncode = subprocess.call(args)
    print 'returncode == %s' % returncode
    if returncode != 0:
        sys.exit(returncode)


def environ(arg):
    return os.environ.get(arg)


def get_script_path():
    return os.path.dirname(os.path.realpath(sys.argv[0]))


def main():
    travis_pull_request = environ('TRAVIS_PULL_REQUEST')
    print 'travis_pull_request == %s' % travis_pull_request
    if travis_pull_request == 'true':
        buildDebug()
    else:
        buildRelease()


def buildDebug():
    call(['./gradlew', ':app:assembleDebug'])


def buildRelease():
    release_name = None
    travis_branch = environ('TRAVIS_BRANCH')
    print 'travis_branch == %s' % travis_branch
    if travis_branch == 'master':
        travis_tag = environ('TRAVIS_TAG')
        print 'travis_tag == %s' % travis_tag
        if travis_tag:
            response = requests.get('https://api.github.com/repos/swooby/alfred/releases/tags/%s' % travis_tag)
            response = response.json()
            print 'response == %s' % response
            release_name = response.get('name')
    print 'release_name == %s' % release_name

    call(['./gradlew', ':app:assembleRelease',
          '-PKEYSTORE=%s' % environ('KEYSTORE'),
          '-PKEYSTORE_PASSWORD=%s' % environ('KEYSTORE_PASSWORD'),
          '-PKEY_ALIAS=%s' % environ('KEY_ALIAS'),
          '-PKEY_PASSWORD=%s' % environ('KEY_PASSWORD')])

    if not release_name:
        return

    uploadGooglePlay()
    uploadFirebase()


def uploadGooglePlay():
    script_path = get_script_path()
    print 'script_path == %s' % script_path
    packageName = 'com.swooby.alfred'
    print 'packageName == %s' % packageName
    track = 'alpha'
    print 'track == %s' % track
    keyFilename = '%s/../Swooby Play Android Dev-159296a07371.json' % script_path
    print 'keyFilename == %s' % keyFilename
    apkFilename = '%s/../app/build/outputs/apk/swooby-android-app-alfred-release.apk' % script_path
    print 'apkFilename == %s' % apkFilename
    mappingFilename = '%s/../app/build/outputs/mapping/release/mapping.txt' % script_path
    print 'mappingFilename == %s' % mappingFilename

    credentials = ServiceAccountCredentials.from_json_keyfile_name(keyFilename)
    service = build('androidpublisher', 'v2', credentials=credentials)
    serviceEdits = service.edits()

    result = serviceEdits.insert(
        packageName=packageName,
        body={}) \
        .execute()
    editId = result['id']

    result = serviceEdits.apks().upload(
        editId=editId,
        packageName=packageName,
        media_body=apkFilename) \
        .execute()
    versionCode = result['versionCode']
    print 'Uploaded APK file versionCode=%d, path=%s' % (versionCode, apkFilename)

    if mappingFilename:
        mediaUpload = MediaFileUpload(mappingFilename, mimetype='application/octet-stream')
        serviceEdits.deobfuscationfiles().upload(
            editId=editId,
            packageName=packageName,
            deobfuscationFileType='proguard',
            apkVersionCode=versionCode,
            media_body=mediaUpload) \
            .execute()
        print 'Uploaded mapping file versionCode=%d, path=%s' % (versionCode, mappingFilename)

    result = serviceEdits.tracks().update(
        editId=editId,
        packageName=packageName,
        track=track,
        body={u'versionCodes': [versionCode]}) \
        .execute()
    print 'Track %s set for versionCode=%d' % (result['track'], versionCode)

    serviceEdits.commit(
        editId=editId,
        packageName=packageName) \
        .execute()
    print 'Committed editId %s' % editId


def uploadFirebase():
    script_path = get_script_path()
    print 'script_path == %s' % script_path
    FirebaseServiceAccountFilePath = '%s/../alfred-mobile-firebase-crashreporting-nlf98-ef1a85c614.json' % script_path
    call(['./gradlew', ':app:firebaseUploadReleaseProguardMapping',
          '-PFirebaseServiceAccountFilePath=%s' % FirebaseServiceAccountFilePath,
          '-PKEYSTORE=%s' % environ('KEYSTORE'),
          '-PKEYSTORE_PASSWORD=%s' % environ('KEYSTORE_PASSWORD'),
          '-PKEY_ALIAS=%s' % environ('KEY_ALIAS'),
          '-PKEY_PASSWORD=%s' % environ('KEY_PASSWORD')])


if __name__ == '__main__':
    main()
