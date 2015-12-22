function f=prodDist(z,lamda,p)
% Distribution of the product of a geometrical and an exponential distribution
pn= 1-p;
lz= lamda*z;
f= zeros(size(z));
for n=1:20
	f= f+pn^n/n*exp(-lz/n);
end
f= f*lamda*p/pn;
