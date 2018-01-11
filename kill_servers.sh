#!/bin/csh
ssh $USER@ug135.eecg.toronto.edu <<'ENDSSH'

cd ece419/m2/ScalableStorageService-stub

set pids = ".running_servers"
if (-e $pids) then
	foreach line ( "`cat .running_servers`" )
		echo "Killing previous server at port $line"
  		kill $line
	end
	rm -f $pids
endif

ENDSSH
