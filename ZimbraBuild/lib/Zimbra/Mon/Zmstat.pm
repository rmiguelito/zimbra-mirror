# 
# ***** BEGIN LICENSE BLOCK *****
# 
# Portions created by Zimbra are Copyright (C) 2007 Zimbra, Inc.
# All Rights Reserved.
# 
# The Original Code is: Zimbra Network
# 
# ***** END LICENSE BLOCK *****
# 

package Zimbra::Mon::Zmstat;

use Exporter;
@ISA = qw(Exporter);
@EXPORT = qw(
    zmstatInit getZimbraHome getZmstatRoot
    percent getTstamp getDate waitUntilNiceRoundSecond
    getPidFileDir readPidFile createPidFile
    getLogFilePath openLogFile rotateLogFile
    readLine
);

use strict;
use File::Basename;
use FileHandle;

my %LC;

sub getLocalConfig(;@) {
    my @vars = @_;
    my $dir = dirname($0);
    my $cmd = "zmlocalconfig -q -x";
    if (scalar(@vars) > 0) {
        $cmd .= ' ' . join(' ', @vars);
    }
    open(LCH, "$cmd |") or die "Unable to invoke $cmd: $!";
    my $line;
    while (defined($line = <LCH>)) {
        $line =~ s/[\r\n]*$//;  # Remove trailing CR/LFs.
        my @fields = split(/\s*=\s*/, $line, 2);
        $LC{$fields[0]} = $fields[1];
    }
    close(LCH);
}

sub userCheck() {
    my $loggedIn = `id -un`;
    chomp($loggedIn) if (defined($loggedIn));
    my $expected = $LC{zimbra_user};
    if ($loggedIn ne $expected) {
        print STDERR "Must be user $expected to run this command\n";
        exit(1);
    }
}

sub osCheck() {
    if ($^O !~ /linux/i) {
        print "zmstat is supported on Linux only\n";
        exit(0);  # return success to calling script
    }
}

sub zmstatInit() {
    osCheck();
    getLocalConfig('zimbra_home', 'zimbra_user', 'zmstat_log_directory');
    userCheck();
}

sub getZimbraHome() {
    return $LC{'zimbra_home'};
}

sub getZmstatRoot() {
    return $LC{'zmstat_log_directory'};
}

sub percent($$) {
    my ($val, $total) = @_;
    return sprintf("%.1f", $total > 0 ? $val * 100 / $total : 0);
}

sub getTstamp() {
    my ($sec, $min, $hour, $mday, $mon, $year) =
        localtime();
    return sprintf("%02d/%02d/%04d %02d:%02d:%02d",
                   $mon + 1, $mday, $year + 1900,
                   $hour, $min, $sec);
}

sub getDate() {
    my ($sec, $min, $hour, $mday, $mon, $year) = localtime();
    return sprintf("%04d-%02d-%02d", $year + 1900, $mon + 1, $mday);
}

sub waitUntilNiceRoundSecond($) {
    my $interval = shift;
    $interval %= 3600;
    while (1) {
        my ($sec, $min) = localtime();
        my $t = $min * 60 + $sec;
        my $howlong = $t % $interval;
        last if ($howlong == 0);
        select(undef, undef, undef, 0.05);
    }
    return time;
}

sub getPidFileDir() {
    return getZmstatRoot() . "/pid";
}

sub readPidFile($) {
    my $file = shift;
    my $pid = undef;
    if (open(PID, "< $file")) {
        $pid = <PID>;
        close(PID);
        chomp($pid) if (defined($pid));
    }
    return $pid;
}

# Check pid file to see if this process is a duplicate.
# If not, create the pid file.
sub createPidFile($) {
    my $name = shift;
    my $zmstatDir = getZmstatRoot();
    my $pidDir = getPidFileDir();
    my $pidFile = "$pidDir/$name";
    if (-e $pidFile) {
        my $pid = readPidFile($pidFile);
        if ($pid) {
            if (kill(0, $pid)) {
                # Already running.
                print STDERR "$name: Already running as pid $pid\n";
                exit(0);
            }
            unlink($pidFile);
        }
    }
    if (! -e $zmstatDir) {
        die "$zmstatDir does not exist";
    }
    if (! -e $pidDir) {
        mkdir($pidDir, 0755);
    }
    open(PID, "> $pidFile") || die "Unable to create pid file $pidFile: $!";
    print PID "$$\n";
    close(PID);
}

sub getLogFilePath($) {
    my $fname = shift;
    return getZmstatRoot() . "/$fname";
}

sub openLogFile($;$) {
    my ($logfile, $heading) = @_;
    my $fh = new FileHandle;
    if (defined($logfile) && $logfile ne '' && $logfile ne '-') {
        my $dir = File::Basename::dirname($logfile);
        if (! -e $dir) {
            mkdir($dir, 0755) || die "Unable to create log directory $dir: $!";
        }
        $fh->open(">> $logfile") || die "Unable to open log file $logfile: $!";
    } else {
        $fh = *STDOUT;
    }
    if ($heading) {
        $fh->print($heading);
        $fh->print("\n");
        $fh->flush();
    }
    return $fh;
}

sub rotateLogFile($$;$$) {
    my ($fh, $logfile, $heading, $date) = @_;
    my ($name, $path) = File::Basename::fileparse($logfile);
    if (!defined($date)) {
        $date = getDate();
    }
    my $rotatedir = "$path/$date";
    mkdir($rotatedir, 0755);
    if (! -d $rotatedir) {
        die "Unable to create log rotation directory $rotatedir";
    }
    $fh->close();

    my $rotatefile = "$rotatedir/$name";

    # If previous .gz is there, unzip it.
    my $rotateGz = "$rotatefile.gz";
    if (-e $rotateGz) {
    	if (-e $rotatefile) {
    	    unlink($rotatefile);
    	}
    	system("gzip -d $rotateGz");
    }

    # Rename or concatenate, with gzip.
    if (! -e $rotatefile) {
        my $rc = system("cat $logfile | gzip -c > $rotateGz");
        $rc >>= 8;
        if ($rc) {
        	die "Unable to move $logfile to $rotateGz";
        }
        unlink($logfile);
    } else {
        my $rc = system("cat $rotatefile $logfile | gzip -c > $rotateGz");
        $rc >>= 8;
        if ($rc) {
            die "Unable to concatenate $logfile and $rotatefile to $rotateGz";
        }
        unlink($rotatefile, $logfile);
    }

    return openLogFile($logfile, $heading);
}

sub readLine($$) {
    my ($rh, $skip_empty) = @_;
    my $line = '';
    while ($line eq '') {
        $line = <$rh>;
        return if (!defined($line));  # EOF
        chomp($line);
        last if (!$skip_empty);
    }
    return $line;
}

1;
