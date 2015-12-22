function equiprobHist(data)

l= length(data);
data= sort(data);
bins= ceil(sqrt(l)/2);
binx= zeros([bins,1]);
for i=1:bins
	binx(i)= data(ceil(i*l/bins-l/bins/2));
end
hist(data,binx);
%[h,~]= hist(data,binx);
%bar(binx,h/trapz(binx,h));

