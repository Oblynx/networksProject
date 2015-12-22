close all;
clear all;
%% Get data 8
%{
load('log8_speed8000.mat');
transferTimeARQ= (arques8(:,2)-arques8(:,1));%.*(arques8(:,4)+1);
transferTimeECH= echoes8(:,2)-echoes8(:,1);
%Echoes first
reactTimeECH= echoes8(:,3);
reactTimeARQ= arques8(:,3);
retries= arques8(:,4);
clear('echoes8','arques8');
%}
%% Get data 9
load('log9_sp8000.mat');
transferTimeARQ= arques9sp8000(:,2)-arques9sp8000(:,1);
transferTimeECH= echoes9sp8000(:,2)-echoes9sp8000(:,1);
reactTimeECH= echoes9sp8000(:,3);
reactTimeARQ= arques9sp8000(:,3);
retries= arques9sp8000(:,4);
clear('echo*','arqu*');
%% remove total outliers
outliers=3;
transferTimeARQ= sort(transferTimeARQ);
transferTimeARQ= transferTimeARQ(1:end-outliers);
transferTimeECH= sort(transferTimeECH);
transferTimeECH= transferTimeECH(1:end-outliers);
reactTimeECH= sort(reactTimeECH);
reactTimeECH= reactTimeECH(1:end-outliers);
reactTimeARQ= sort(reactTimeARQ);
reactTimeARQ= reactTimeARQ(1:end-outliers);
retries= sort(retries);
retries= retries(1:end-outliers);
reactTime= [reactTimeECH;reactTimeARQ];
%% Histograms
figure(1);
logHist(reactTime);
title('Reaction time: before calling modem.write until receiving 1st byte');
xlabel('t(ms)');
grid minor;
%
figure(2);
hist(transferTimeECH,length(unique(transferTimeECH)));
title('G1: E6996, 21/12, ~23:00 -- ECHO packet transfer time');
xlabel('t(ms)');
grid minor;
figure(3);
%hist(transferTimeARQ,sqrt(length(transferTimeARQ)));
logHist(transferTimeARQ);
title('G2: Q4761-R7581, 21/12, ~23:00 -- ARQ packet transfer time');
xlabel('t(ms)');
grid minor;
%
figure(4);
logHist(transferTimeECH+reactTimeECH);
title('G1: E6996, 21/12, ~23:00 -- ECHO packet total transfer time');
xlabel('t(ms)');
grid minor;
figure(5);
logHist(transferTimeARQ+reactTimeARQ);
title('G2: Q4761-R7581, 21/12, ~23:00 -- ARQ packet total transfer time');
xlabel('t(ms)');
grid minor;
%
figure(6);
[f,x]=hist(retries,length(unique(retries)));
bar(0:4,f);
title('G3: Q4761-R7581, 21/12, ~23:00 -- Retries per packet');
grid minor;
%}
