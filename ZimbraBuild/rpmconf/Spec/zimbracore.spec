#
# spec file for zimbra.rpm
#
Summary: Zimbra Core
Name: zimbra-core
Version: @@VERSION@@
Release: @@RELEASE@@
Copyright: Various
Group: Applications/Messaging
URL: http://www.zimbra.com
Vendor: Zimbra, Inc.
Packager: Zimbra, Inc.
BuildRoot: /opt/zimbra
AutoReqProv: no
requires: libidn
requires: curl
requires: fetchmail
requires: openssl
requires: gmp

%description
Best email money can buy

%prep

%build

%install

%pre

%post

%preun

%postun

%files
