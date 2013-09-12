#version 1.0

package pplive;
use String::CRC32;

sub getGuid {
    my $uri = shift;
    my $roff = rindex($uri, ".");
    my $suffix = substr($uri, $roff+1);

    my $guid;
    SWITCH: foreach($suffix) {
        /^block$/i  &&  do { $guid=getBlockGuid($uri); last SWITCH; };
        /^m3u$/i    &&  do { $guid=getM3u8Guid($uri); last SWITCH; };
        /^m3u8$/i   &&  do { $guid=getM3u8Guid($uri); last SWITCH; };
        /^ts$/i     &&  do { $guid=getTsGuid($uri); last SWITCH; };
        /^fragment$/i     &&  do { $guid=getFragmentGuid($uri); last SWITCH; };
    }

    return $guid;
}

sub getBlockGuid {
	my $uri = shift;
	@array = split(/\//, $uri);
	my $guid = @array[2];
	if ( @array[1] eq "cdn") {
		$guid = @array[3];
	}

	return lc($guid);
}

sub getM3u8Guid {
	my $uri = shift;
	@array = split(/\//, $uri);
	my $guid = substr(@array[4], 0, 32);

	return lc($guid);
}

sub getTsGuid {
	my $uri = shift;
	@array = split(/\//, $uri);
	my $guid = @array[2];

	return lc($guid);
}

sub getFragmentGuid {
	my $uri = shift;
	@array = split(/\//, $uri);
	my $guid = @array[4];

	return lc($guid);
}

sub getUpstreamPeer {
	my $guid = shift;
	$crc = crc32($guid);

	$index = ( ( $crc >> 16 ) & 0x7fff ) % 2;

	return $index;
}

#print getGuid('/live/e8b6a5851b1148baa1f49d643accd6fc/1337325195.block'), "\n";
#print getGuid('/live/5/60/e8b6a5851b1148baa1f49d643accd6fc.m3u'), "\n";
#print getGuid('/live/5/60/e8b6a5851b1148baa1f49d643accd6fc.m3u8'), "\n";
#print getGuid('/live/e8b6a5851b1148baa1f49d643accd6fc/1337325195.ts'), "\n";
#print getGuid('/live/5/60/ac9457f5aa2c4ed48c5c2d8c9c0678d6/video/13539232700000000.fragment'), "\n";

#print getUpstreamPeer('622698d290214399bc83dad5cdee2168');
#print getUpstreamPeer('238baa4ac12f427896cecf12234159ef');
#print getUpstreamPeer('a19f5da93b8b4d8e9f0a489447ae1324');
#print getUpstreamPeer('e4c8a2dc060b4f729bb8ce73df08c0d9');

#print getUpstreamPeer('0b94e35aa6be48b5b749f3fe160efc5e');
#print getUpstreamPeer('44919ebf659e4a15b02b0e3328a03e6c');
#print getUpstreamPeer('52e02f0ca77546c8a3901f996667d57b');
#print getUpstreamPeer('663e4e3677674b15925a1dbca836cdbe');
#print getUpstreamPeer('ba776e850d4548e9b6ee508f9ccc4642');

1;
__END__
