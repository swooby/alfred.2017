#!/usr/bin/python

"""Uploads an apk to Google Play"""

import argparse
from googleapiclient.discovery import build
from oauth2client.service_account import ServiceAccountCredentials

argparser = argparse.ArgumentParser()
argparser.add_argument('--track', choices=['alpha', 'beta', 'production', 'rollout'],
                       default='alpha',
                       help='The release to target; defaults to alpha')
argparser.add_argument('--keyFilename',
                       help='The path to the .json private key file for the Service Account'
                            '; if empty then the environment variable GOOGLE_APPLICATION_CREDENTIALS will be used'
                            '; for more info see https://developers.google.com/identity/protocols/application-default-credentials')
argparser.add_argument('--packageName',
                       required=True,
                       help='The APK\'s package name; example: com.android.sample')
argparser.add_argument('--apkFilename',
                       required=True,
                       help='The path to the APK file to upload')


def main():
    flags = argparser.parse_args()
    track = flags.track
    packageName = flags.packageName
    keyFilename = flags.keyFilename
    apkFilename = flags.apkFilename

    credentials = ServiceAccountCredentials.from_json_keyfile_name(keyFilename)
    service = build('androidpublisher', 'v2', credentials=credentials)

    result = service.edits().insert(
        packageName=packageName,
        body={}) \
        .execute()
    editId = result['id']

    apk_response = service.edits().apks().upload(
        editId=editId,
        packageName=packageName,
        media_body=apkFilename) \
        .execute()
    versionCode = apk_response['versionCode']
    print 'Version code %d has been uploaded' % versionCode

    track_response = service.edits().tracks().update(
        editId=editId,
        packageName=packageName,
        track=track,
        body={u'versionCodes': [versionCode]}) \
        .execute()
    print 'Track %s is set for version code(s) %s' % (
        track_response['track'], str(track_response['versionCodes']))

    commit_request = service.edits().commit(
        editId=editId,
        packageName=packageName) \
        .execute()
    print 'Edit "%s" has been committed' % (commit_request['id'])


if __name__ == '__main__':
    main()
