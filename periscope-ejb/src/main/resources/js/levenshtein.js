function(sa,sb) {
	var cost;
	
	// get values
	var a = sa;
	var m = a.length;
	
	var b = sb;
	var n = b.length;
	
	// make sure a.length >= b.length to use O(min(n,m)) space, whatever that is
	if (m < n) {
		var c=a;a=b;b=c;
		var o=m;m=n;n=o;
	}
	
	var r = new Array();
	r[0] = new Array();
	for (var c = 0; c < n+1; c++) {
		r[0][c] = c;
	}
	
	for (var i = 1; i < m+1; i++) {
		r[i] = new Array();
		r[i][0] = i;
		for (var j = 1; j < n+1; j++) {
			cost = (a.charAt(i-1) == b.charAt(j-1))? 0: 1;
			x = r[i-1][j]+1;
			y = r[i][j-1]+1;
			z = r[i-1][j-1]+cost;
			if( x < y && x < z ) {
				r[i][j] = x;
			}else if (y < x && y < z){
				r[i][j] = y;
			}else {
				r[i][j] = z
			}
		}
	}
	
	return r[m][n];
}