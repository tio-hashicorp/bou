#!/bin/bash
keytool -genkeypair -keyalg RSA -keysize 2048 -alias 'lxpoctsgv860.tst.sg.uobnet.com' -ext SAN=dns:lxpoctsgv860.tst.sg.uobnet.com,dns:lxpoctsgv860.sg.uobnet.com -keystore keys.p12 -storetype pkcs12 -validity 720 -keysize 2048
