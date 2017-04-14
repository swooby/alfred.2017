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
import re
import requests
import subprocess
import sys
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload
from oauth2client.service_account import ServiceAccountCredentials


def call(args):
    print 'call(%r)' % args
    returncode = subprocess.call(args)
    print 'returncode == %r' % returncode
    if returncode != 0:
        sys.exit(returncode)


def environ(arg):
    return os.environ.get(arg)


def get_script_path():
    return os.path.dirname(os.path.realpath(sys.argv[0]))


def is_release():
    result = False
    travis_pull_request = environ('TRAVIS_PULL_REQUEST')
    print 'travis_pull_request == %r' % travis_pull_request
    travis_branch = environ('TRAVIS_BRANCH')
    print 'travis_branch == %r' % travis_branch
    travis_tag = environ('TRAVIS_TAG')
    print 'travis_tag == %r' % travis_tag
    if (travis_pull_request == 'false' and travis_branch == travis_tag and
            re.match('^v\d+\.\d+(\.\d+)?(-\S*)?$', travis_branch)):
        response = requests.get('https://api.github.com/repos/swooby/alfred/releases/tags/%s' % travis_tag)
        response = response.json()
        # print 'response == %r' % response
        release_name = response.get('name')
        print 'release_name == %r' % release_name
        result = travis_branch == travis_tag == release_name

    return result


def uploadGooglePlay():
    scriptPath = get_script_path()
    print 'scriptPath == %r' % scriptPath
    packageName = 'com.swooby.alfred'
    print 'packageName == %r' % packageName
    track = 'alpha'
    print 'track == %r' % track
    keyFilename = '%s/../Swooby Play Android Dev-159296a07371.json' % scriptPath
    print 'keyFilename == %r' % keyFilename
    apkFilename = '%s/../app/build/outputs/apk/swooby-android-app-alfred-release.apk' % scriptPath
    print 'apkFilename == %r' % apkFilename
    mappingFilename = '%s/../app/build/outputs/mapping/release/mapping.txt' % scriptPath
    print 'mappingFilename == %r' % mappingFilename

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
    print 'Uploaded APK file versionCode=%d, path=%r' % (versionCode, apkFilename)

    if mappingFilename:
        mediaUpload = MediaFileUpload(mappingFilename, mimetype='application/octet-stream')
        serviceEdits.deobfuscationfiles().upload(
            editId=editId,
            packageName=packageName,
            deobfuscationFileType='proguard',
            apkVersionCode=versionCode,
            media_body=mediaUpload) \
            .execute()
        print 'Uploaded mapping file versionCode=%d, path=%r' % (versionCode, mappingFilename)

    result = serviceEdits.tracks().update(
        editId=editId,
        packageName=packageName,
        track=track,
        body={u'versionCodes': [versionCode]}) \
        .execute()
    print 'Track %r set for versionCode=%d' % (result['track'], versionCode)

    serviceEdits.commit(
        editId=editId,
        packageName=packageName) \
        .execute()
    print 'Committed editId %r' % editId


def uploadFirebase():
    scriptPath = get_script_path()
    print 'scriptPath == %r' % scriptPath
    FirebaseServiceAccountFilePath = '%s/../alfred-mobile-firebase-crashreporting-nlf98-ef1a85c614.json' % scriptPath
    call(['./gradlew', ':app:firebaseUploadReleaseProguardMapping',
          '-PFirebaseServiceAccountFilePath=%s' % FirebaseServiceAccountFilePath,
          '-PKEYSTORE=%s' % environ('KEYSTORE'),
          '-PKEYSTORE_PASSWORD=%s' % environ('KEYSTORE_PASSWORD'),
          '-PKEY_ALIAS=%s' % environ('KEY_ALIAS'),
          '-PKEY_PASSWORD=%s' % environ('KEY_PASSWORD')])


def main():
    isRelease = is_release()
    print 'isRelease == %r' % isRelease

    f = open('alfred.properties', 'w')
    f.write('ALFRED_IS_RELEASE=%s' % ('true' if isRelease else 'false'))
    f.close()

    command = ['./gradlew', ':app:assembleRelease']
    if isRelease:
        command.extend(['-PKEYSTORE=%s' % environ('KEYSTORE'),
                        '-PKEYSTORE_PASSWORD=%s' % environ('KEYSTORE_PASSWORD'),
                        '-PKEY_ALIAS=%s' % environ('KEY_ALIAS'),
                        '-PKEY_PASSWORD=%s' % environ('KEY_PASSWORD')])
    call(command)

    if not isRelease:
        return

    uploadGooglePlay()
    uploadFirebase()


if __name__ == '__main__':
    main()
