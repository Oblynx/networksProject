%% Java PID control
clear all;
javaaddpath('teiresias.jar');
s= IthakiSocket(48014,38014,3000);
flightlevel= 150;


s.initAutopilot();
s.sendAutopilot(flightlevel, 100, 100);
while true
  resp= s.rcvAutopilot();
  %System.out.print("[controller]: status="+new String(resp.bytes()));
  
  %u=pid.next(err);
  
  %System.out.println("[controller]: u="+u+" err="+(flightlevel-resp.altit)+"\n");
  s.sendAutopilot(flightlevel, u, u);
end
